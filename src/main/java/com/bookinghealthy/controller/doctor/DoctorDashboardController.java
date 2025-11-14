package com.bookinghealthy.controller.doctor;

import com.bookinghealthy.model.Booking;
import com.bookinghealthy.model.BookingStatus;
import com.bookinghealthy.model.Doctor;
import com.bookinghealthy.repository.BookingRepository; // Dùng BookingRepository
import com.bookinghealthy.service.BookingService;
import com.bookinghealthy.service.DoctorService;
import com.bookinghealthy.service.EmailService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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

    // Hàm trợ giúp: Lấy Doctor entity từ User đang đăng nhập
    private Doctor getLoggedInDoctor(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return doctorService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Doctor not found for current user"));
    }

    // 1. HIỂN THỊ DANH SÁCH LỊCH HẸN (CHO BÁC SĨ)
    @GetMapping("/dashboard")
    public String doctorDashboard(Model model, Authentication authentication) {
        // Lấy Doctor đang đăng nhập
        Doctor currentDoctor = getLoggedInDoctor(authentication);

        // Lấy danh sách lịch hẹn CHỈ CỦA BÁC SĨ ĐÓ
        List<Booking> myBookings = bookingRepository.findByDoctor(currentDoctor);

        model.addAttribute("listBookings", myBookings);

        // Trỏ đến file dashboard của bạn
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
}