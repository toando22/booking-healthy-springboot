package com.bookinghealthy.controller.admin;

import com.bookinghealthy.model.Booking;
import com.bookinghealthy.model.BookingStatus;
import com.bookinghealthy.service.BookingService;
import com.bookinghealthy.service.EmailService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/manage-booking") // Khớp với link trong sidebar
public class AdminBookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private EmailService emailService;

    // 1. HIỂN THỊ DANH SÁCH (READ)
    @GetMapping
    public String listBookings(Model model) {
        // Dùng hàm findAll() đã được tối ưu (@EntityGraph)
        model.addAttribute("listBookings", bookingService.findAll());
        return "admin/booking-list"; // -> Tới file HTML
    }

    // 2. XỬ LÝ XÁC NHẬN (CONFIRM)
    @GetMapping("/confirm/{id}")
    public String confirmBooking(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Booking booking = bookingService.findById(id)
                    .orElseThrow(() -> new Exception("Không tìm thấy Lịch hẹn ID: " + id));

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingService.save(booking);

            // Gửi mail cho Bệnh nhân báo "Đã xác nhận"
            emailService.sendBookingConfirmation(booking);

            ra.addFlashAttribute("successMessage", "Đã xác nhận lịch hẹn thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/manage-booking";
    }

    // 3. XỬ LÝ HỦY LỊCH (CANCEL)
    @Transactional // <-- THÊM ANNOTATION NÀY
    @GetMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            Booking booking = bookingService.findById(id)
                    .orElseThrow(() -> new Exception("Không tìm thấy Lịch hẹn ID: " + id));

            booking.setStatus(BookingStatus.CANCELED);
            bookingService.save(booking);

            // Gửi mail cho Bệnh nhân báo "Đã hủy"
            String reason = "Lịch hẹn đã được hủy bởi Quản trị viên hệ thống.";
            emailService.sendBookingCancellation(booking, reason);

            ra.addFlashAttribute("successMessage", "Đã hủy lịch hẹn thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/manage-booking";
    }

    // 4. XỬ LÝ XÓA (DELETE)
    @GetMapping("/delete/{id}")
    public String deleteBooking(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            bookingService.deleteById(id);
            ra.addFlashAttribute("successMessage", "Đã xóa lịch hẹn vĩnh viễn.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi xóa lịch hẹn.");
        }
        return "redirect:/admin/manage-booking";
    }
}