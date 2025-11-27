package com.bookinghealthy.controller.user;

import com.bookinghealthy.model.Booking;
import com.bookinghealthy.model.MedicalRecord;
import com.bookinghealthy.model.User;
import com.bookinghealthy.repository.BookingRepository;
import com.bookinghealthy.repository.MedicalRecordRepository;
import com.bookinghealthy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user/medical-record")
public class UserMedicalRecordController {

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserService userService;

    // Helper: Lấy user đang đăng nhập
    private User getLoggedInUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/view/{bookingId}")
    public String viewMedicalRecord(@PathVariable("bookingId") Long bookingId,
                                    Model model,
                                    Authentication authentication,
                                    RedirectAttributes ra) {

        User currentUser = getLoggedInUser(authentication);

        // 1. Tìm Booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // 2. CHECK BẢO MẬT: Người xem phải là chủ của lịch hẹn này
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            ra.addFlashAttribute("errorMessage", "Bạn không có quyền xem hồ sơ bệnh án này.");
            return "redirect:/profile"; // Hoặc trang lịch sử
        }

        // 3. Tìm Hồ sơ bệnh án
        MedicalRecord record = medicalRecordRepository.findByBookingId(bookingId)
                .orElse(null);

        if (record == null) {
            ra.addFlashAttribute("errorMessage", "Hồ sơ bệnh án chưa được cập nhật. Vui lòng liên hệ phòng khám.");
            return "redirect:/profile";
        }

        model.addAttribute("record", record);
        model.addAttribute("booking", booking);

        // === THÊM 2 DÒNG NÀY ===
        model.addAttribute("role", "USER"); // Đánh dấu người xem là User
        model.addAttribute("backLink", "/profile"); // Quay lại trang hồ sơ

        return "user/medical-record-detail";
    }
}