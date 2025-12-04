package com.bookinghealthy.controller.doctor;

import com.bookinghealthy.model.*;
import com.bookinghealthy.repository.BookingRepository;
import com.bookinghealthy.service.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/doctor")
public class DoctorDashboardController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private DoctorBlockTimeService doctorBlockTimeService;

    @Autowired
    private ReviewService reviewService; // Service lấy đánh giá

    // Helper: Lấy bác sĩ hiện tại
    private Doctor getLoggedInDoctor(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return doctorService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Doctor not found for current user"));
    }

    // ============================================================
    // 1. DASHBOARD CHÍNH (ĐÃ SỬA: GOM NHÓM LỊCH + FILTER NGÀY)
    // ============================================================
    @GetMapping("/dashboard")
    public String doctorDashboard(
            @RequestParam(value = "range", defaultValue = "7") String range,
            Model model,
            Authentication authentication) {

        Doctor currentDoctor = getLoggedInDoctor(authentication);

        // --- A. XỬ LÝ BỘ LỌC THỜI GIAN ---
        LocalDate today = LocalDate.now();
        LocalDate startDate;

        if ("30".equals(range)) {
            startDate = today.minusDays(30);
        } else if ("all".equals(range)) {
            startDate = LocalDate.of(2000, 1, 1);
        } else {
            startDate = today.minusDays(7); // Mặc định 7 ngày
        }

        // --- B. LẤY SỐ LIỆU THỐNG KÊ ---
        List<Booking> allBookings = bookingRepository.findByDoctor(currentDoctor);

        // Lọc danh sách theo ngày để tính toán cho Biểu đồ
        List<Booking> filteredBookings = allBookings.stream()
                .filter(b -> !b.getAppointmentDate().isBefore(startDate) && !b.getAppointmentDate().isAfter(today))
                .collect(Collectors.toList());

        long countPending = filteredBookings.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();
        long countConfirmed = filteredBookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long countCompleted = filteredBookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long countCancelled = filteredBookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELED).count();

        // Khách hôm nay (Luôn lấy theo ngày thực tế)
        long countToday = allBookings.stream()
                .filter(b -> b.getAppointmentDate().equals(today) && b.getStatus() == BookingStatus.CONFIRMED)
                .count();

        model.addAttribute("countPending", countPending);
        model.addAttribute("countConfirmed", countConfirmed);
        model.addAttribute("countCompleted", countCompleted);
        model.addAttribute("countCancelled", countCancelled);
        model.addAttribute("countToday", countToday);
        model.addAttribute("currentRange", range);

        // --- C. WIDGET: ĐÁNH GIÁ MỚI NHẤT ---
        // 4. LẤY DỮ LIỆU ĐÁNH GIÁ (THÊM ĐOẠN NÀY)
        // a. Đánh giá mới nhất (Đã có)
        List<Review> recentReviews = reviewService.getRecentReviews(currentDoctor.getId());

        // b. Điểm trung bình (MỚI)
        Double avgRating = reviewService.getAverageRating(currentDoctor.getId());

        // c. Phân bố sao (MỚI - Để vẽ biểu đồ)
        List<Integer> ratingDist = reviewService.getRatingDistribution(currentDoctor.getId());

        // ... (Code lấy lịch trực giữ nguyên) ...

        // Gửi ra Model
        model.addAttribute("recentReviews", recentReviews);
        model.addAttribute("avgRating", avgRating); // <-- Gửi điểm TB
        model.addAttribute("ratingDist", ratingDist); // <-- Gửi dữ liệu biểu đồ

        // --- D. WIDGET: LỊCH TRỰC (LOGIC MỚI: GOM NHÓM THEO THỨ) ---
        List<Schedule> rawSchedule = doctorService.getDoctorSchedules(currentDoctor.getId());

        // Sử dụng LinkedHashMap để giữ thứ tự Thứ 2 -> CN
        Map<String, List<Schedule>> weeklyScheduleMap = new LinkedHashMap<>();

        DayOfWeek[] days = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        };

        for (DayOfWeek day : days) {
            // Lấy các ca của ngày này và sắp xếp theo giờ
            List<Schedule> shifts = rawSchedule.stream()
                    .filter(s -> s.getDayOfWeek() == day)
                    .sorted(Comparator.comparing(Schedule::getStartTime))
                    .collect(Collectors.toList());

            if (!shifts.isEmpty()) {
                // Convert Enum sang Tiếng Việt làm Key cho Map
                String dayName = switch (day) {
                    case MONDAY -> "Thứ 2";
                    case TUESDAY -> "Thứ 3";
                    case WEDNESDAY -> "Thứ 4";
                    case THURSDAY -> "Thứ 5";
                    case FRIDAY -> "Thứ 6";
                    case SATURDAY -> "Thứ 7";
                    case SUNDAY -> "Chủ nhật";
                };
                weeklyScheduleMap.put(dayName, shifts);
            }
        }
        model.addAttribute("weeklyScheduleMap", weeklyScheduleMap);

        model.addAttribute("activePage", "dashboard");
        return "doctor/dashboard";
    }

    // ============================================================
    // 2. XỬ LÝ LỊCH HẸN (CONFIRM / CANCEL)
    // ============================================================
    @GetMapping("/bookings/confirm/{id}")
    public String confirmBooking(@PathVariable("id") Long id, Authentication authentication, RedirectAttributes ra) {
        try {
            Doctor currentDoctor = getLoggedInDoctor(authentication);
            Booking booking = bookingService.findById(id)
                    .orElseThrow(() -> new Exception("Không tìm thấy Lịch hẹn"));

            if (!booking.getDoctor().getId().equals(currentDoctor.getId())) {
                ra.addFlashAttribute("errorMessage", "Lỗi: Bạn không có quyền xác nhận lịch hẹn này.");
                return "redirect:/doctor/dashboard";
            }

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingService.save(booking);
            emailService.sendBookingConfirmation(booking);
            ra.addFlashAttribute("successMessage", "Đã xác nhận lịch hẹn thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        // Redirect về trang trước đó (nếu gọi từ Dashboard hoặc Booking Request)
        // Ở đây mặc định về Dashboard hoặc Booking Requests đều ổn
        return "redirect:/doctor/booking-requests";
    }

    @Transactional
    @GetMapping("/bookings/cancel/{id}")
    public String cancelBooking(@PathVariable("id") Long id, Authentication authentication, RedirectAttributes ra) {
        try {
            Doctor currentDoctor = getLoggedInDoctor(authentication);
            Booking booking = bookingService.findById(id)
                    .orElseThrow(() -> new Exception("Không tìm thấy Lịch hẹn"));

            if (!booking.getDoctor().getId().equals(currentDoctor.getId())) {
                ra.addFlashAttribute("errorMessage", "Lỗi: Bạn không có quyền hủy lịch hẹn này.");
                return "redirect:/doctor/dashboard";
            }

            booking.setStatus(BookingStatus.CANCELED);
            bookingService.save(booking);
            String reason = "Bác sĩ " + currentDoctor.getUser().getFullName() + " đã từ chối lịch hẹn (do lịch bận đột xuất).";
            emailService.sendBookingCancellation(booking, reason);
            ra.addFlashAttribute("successMessage", "Đã từ chối lịch hẹn thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/doctor/booking-requests";
    }

    // ============================================================
    // 3. QUẢN LÝ LỊCH TRỰC & GIỜ BẬN
    // ============================================================

    @GetMapping("/schedule-register")
    public String showScheduleRegister(
            @RequestParam(value = "selectedDate", required = false) LocalDate selectedDate,
            Model model,
            Authentication authentication) {

        Doctor currentDoctor = getLoggedInDoctor(authentication);

        if (selectedDate == null) {
            selectedDate = LocalDate.now();
        }

        DayOfWeek dayOfWeek = selectedDate.getDayOfWeek();
        List<Schedule> mySchedules = doctorService.getDoctorSchedules(currentDoctor.getId());
        List<DoctorBlockTime> myBlockTimes = doctorBlockTimeService.getBlockedSlotsForDoctorAndDate(currentDoctor.getId(), selectedDate);

        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("dayOfWeek", dayOfWeek);
        model.addAttribute("mySchedules", mySchedules);
        model.addAttribute("myBlockTimes", myBlockTimes);
        model.addAttribute("activePage", "schedule");

        return "doctor/schedule-register";
    }

    @PostMapping("/schedule/create")
    public String createSchedule(
            @RequestParam("dayOfWeek") String dayOfWeek,
            @RequestParam("session") String session,
            Authentication authentication,
            RedirectAttributes ra) {

        Doctor currentDoctor = getLoggedInDoctor(authentication);
        String error = doctorService.registerSchedule(currentDoctor, dayOfWeek, session);

        if (error != null) {
            ra.addFlashAttribute("errorMessage", error);
        } else {
            ra.addFlashAttribute("successMessage", "Đăng ký lịch trực thành công!");
        }
        return "redirect:/doctor/schedule-register";
    }

    @PostMapping("/schedule/block")
    public String blockSchedule(
            @RequestParam("blockDate") LocalDate blockDate,
            @RequestParam("startTime") LocalTime startTime,
            @RequestParam("endTime") LocalTime endTime,
            @RequestParam("reason") String reason,
            Authentication authentication,
            RedirectAttributes ra) {

        Doctor currentDoctor = getLoggedInDoctor(authentication);

        if (blockDate.isBefore(LocalDate.now())) {
            ra.addFlashAttribute("errorMessage", "Không thể chặn giờ trong quá khứ.");
            ra.addAttribute("selectedDate", blockDate);
            return "redirect:/doctor/schedule-register";
        }

        String error = doctorBlockTimeService.blockTime(currentDoctor, blockDate, startTime, endTime, reason);

        if (error != null) {
            ra.addFlashAttribute("errorMessage", error);
        } else {
            ra.addFlashAttribute("successMessage", "Đã chặn khung giờ thành công!");
        }

        ra.addAttribute("selectedDate", blockDate);
        return "redirect:/doctor/schedule-register";
    }

    @GetMapping("/schedule/delete/{id}")
    public String deleteSchedule(@PathVariable("id") Long id, RedirectAttributes ra) {
        doctorService.deleteSchedule(id);
        ra.addFlashAttribute("successMessage", "Đã xóa lịch trực.");
        return "redirect:/doctor/schedule-register";
    }

    @GetMapping("/schedule/unblock/{id}")
    public String unblockSchedule(@PathVariable("id") Long id, @RequestParam("date") LocalDate date, RedirectAttributes ra) {
        doctorBlockTimeService.unblockTime(id);
        ra.addFlashAttribute("successMessage", "Đã gỡ chặn khung giờ.");
        ra.addAttribute("selectedDate", date);
        return "redirect:/doctor/schedule-register";
    }
}