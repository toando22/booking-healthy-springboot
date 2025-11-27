package com.bookinghealthy.controller.doctor;

import com.bookinghealthy.model.*;
import com.bookinghealthy.repository.BookingRepository; // Dùng BookingRepository
import com.bookinghealthy.service.BookingService;
import com.bookinghealthy.service.DoctorBlockTimeService;
import com.bookinghealthy.service.DoctorService;
import com.bookinghealthy.service.EmailService;
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
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/doctor") // Đường dẫn gốc cho Bác sĩ
public class DoctorDashboardController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private BookingRepository bookingRepository; // Dùng Repository để gọi hàm findByDoctor

    @Autowired
    private BookingService bookingService; // Dùng Service để Save

    @Autowired
    private EmailService emailService;

    @Autowired
    private DoctorBlockTimeService doctorBlockTimeService;

    // Hàm trợ giúp: Lấy Doctor entity từ User đang đăng nhập
    private Doctor getLoggedInDoctor(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return doctorService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Doctor not found for current user"));
    }

    // 1. HIỂN THỊ DANH SÁCH LỊCH HẸN (CHO BÁC SĨ)
    // === NÂNG CẤP DASHBOARD ===
    @GetMapping("/dashboard")
    public String doctorDashboard(Model model, Authentication authentication) {
        Doctor currentDoctor = getLoggedInDoctor(authentication);

        // 1. Lấy danh sách Booking của bác sĩ này
        List<Booking> myBookings = bookingRepository.findByDoctor(currentDoctor);

        // 2. TÍNH TOÁN SỐ LIỆU THỐNG KÊ (Dùng Stream API cho nhanh)
        long countPending = myBookings.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();
        long countConfirmed = myBookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long countCompleted = myBookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long countCancelled = myBookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELED).count();

        // Tính số khách hôm nay (Confirmed + Today)
        LocalDate today = LocalDate.now();
        long countToday = myBookings.stream()
                .filter(b -> b.getAppointmentDate().equals(today) && b.getStatus() == BookingStatus.CONFIRMED)
                .count();

        // 3. Gửi số liệu ra View
        model.addAttribute("countPending", countPending);
        model.addAttribute("countConfirmed", countConfirmed);
        model.addAttribute("countCompleted", countCompleted);
        model.addAttribute("countCancelled", countCancelled);
        model.addAttribute("countToday", countToday);

        // Gửi danh sách (để hiện bảng bên dưới)
        model.addAttribute("listBookings", myBookings);
        model.addAttribute("activePage", "dashboard"); // Để highlight sidebar

        return "doctor/dashboard";
    }

    // 2. BÁC SĨ XÁC NHẬN LỊCH
    @GetMapping("/bookings/confirm/{id}")
    public String confirmBooking(@PathVariable("id") Long id, Authentication authentication, RedirectAttributes ra) {
        try {
            Doctor currentDoctor = getLoggedInDoctor(authentication);
            Booking booking = bookingService.findById(id)
                    .orElseThrow(() -> new Exception("Không tìm thấy Lịch hẹn"));

            // Check bảo mật: Đảm bảo Bác sĩ này đúng là chủ của lịch hẹn
            if (!booking.getDoctor().getId().equals(currentDoctor.getId())) {
                ra.addFlashAttribute("errorMessage", "Lỗi: Bạn không có quyền xác nhận lịch hẹn này.");
                return "redirect:/doctor/dashboard";
            }

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingService.save(booking);

            emailService.sendBookingConfirmation(booking); // Gửi mail cho Bệnh nhân

            ra.addFlashAttribute("successMessage", "Đã xác nhận lịch hẹn thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/doctor/dashboard";
    }

    // === THAY THẾ TOÀN BỘ HÀM NÀY ===
    @Transactional // <-- THÊM ANNOTATION NÀY Chúng ta sẽ "bảo" 2 hàm "Hủy lịch" phải giữ "phiên" (session) database mở cho đến khi EmailService được gọi xong.
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

            // Kích hoạt gửi mail báo HỦY cho Bệnh nhân
            String reason = "Bác sĩ " + currentDoctor.getUser().getFullName() + " đã từ chối lịch hẹn (có thể do lịch bận đột xuất).";
            emailService.sendBookingCancellation(booking, reason);

            ra.addFlashAttribute("successMessage", "Đã từ chối lịch hẹn thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/doctor/dashboard";
    }
    // ============================================================
    // === PHẦN QUẢN LÝ LỊCH TRỰC & GIỜ BẬN (ĐÃ SỬA CHỮA) ===
    // ============================================================

    // 1. Hiển thị trang Đăng ký
    @GetMapping("/schedule-register")
    public String showScheduleRegister(
            @RequestParam(value = "selectedDate", required = false) LocalDate selectedDate,
            Model model,
            Authentication authentication) {

        Doctor currentDoctor = getLoggedInDoctor(authentication);

        // Mặc định là hôm nay nếu không chọn
        if (selectedDate == null) {
            selectedDate = LocalDate.now();
        }

        // Thông tin ngày/thứ
        DayOfWeek dayOfWeek = selectedDate.getDayOfWeek();

        // Lấy lịch ĐỊNH KỲ
        List<Schedule> mySchedules = doctorService.getDoctorSchedules(currentDoctor.getId());

        // Lấy lịch BẬN ĐỘT XUẤT (Sửa lỗi hiển thị: Lấy Entity thay vì String)
        List<DoctorBlockTime> myBlockTimes = doctorBlockTimeService.getBlockedSlotsForDoctorAndDate(currentDoctor.getId(), selectedDate);

        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("dayOfWeek", dayOfWeek);
        model.addAttribute("mySchedules", mySchedules);
        model.addAttribute("myBlockTimes", myBlockTimes); // Tên biến khớp với HTML
        model.addAttribute("activePage", "schedule");

        return "doctor/schedule-register";
    }

    // 2. Xử lý Đăng ký Lịch Thường xuyên
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

    // 3. Xử lý Chặn Giờ Đột Xuất
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

        // Redirect kèm ngày để giữ nguyên view
        ra.addAttribute("selectedDate", blockDate);
        return "redirect:/doctor/schedule-register";
    }

    // 4. Xóa lịch Thường xuyên
    @GetMapping("/schedule/delete/{id}")
    public String deleteSchedule(@PathVariable("id") Long id, RedirectAttributes ra) {
        doctorService.deleteSchedule(id);
        ra.addFlashAttribute("successMessage", "Đã xóa lịch trực.");
        return "redirect:/doctor/schedule-register";
    }

    // 5. Gỡ Chặn Giờ Đột Xuất
    @GetMapping("/schedule/unblock/{id}")
    public String unblockSchedule(@PathVariable("id") Long id, @RequestParam("date") LocalDate date, RedirectAttributes ra) {
        doctorBlockTimeService.unblockTime(id);
        ra.addFlashAttribute("successMessage", "Đã gỡ chặn khung giờ.");

        ra.addAttribute("selectedDate", date);
        return "redirect:/doctor/schedule-register";
    }
}