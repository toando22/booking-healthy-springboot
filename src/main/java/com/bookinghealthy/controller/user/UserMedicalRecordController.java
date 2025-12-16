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
import org.springframework.security.oauth2.core.user.OAuth2User; // Import quan trọng
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

    // === HÀM LẤY USER CHUẨN (HỖ TRỢ CẢ GOOGLE & LOCAL) ===
    private User getCurrentUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        String usernameOrEmail;

        if (principal instanceof OAuth2User) {
            // Google Login
            usernameOrEmail = ((OAuth2User) principal).getAttribute("email");
        } else if (principal instanceof UserDetails) {
            // Local Login
            usernameOrEmail = ((UserDetails) principal).getUsername();
        } else {
            usernameOrEmail = principal.toString();
        }

        return userService.findByUsername(usernameOrEmail)
                .or(() -> userService.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new RuntimeException("User not found: " + usernameOrEmail));
    }
    // =====================================================

    @GetMapping("/view/{bookingId}")
    public String viewMedicalRecord(@PathVariable("bookingId") Long bookingId,
                                    Model model,
                                    Authentication authentication,
                                    RedirectAttributes ra) {

        try {
            User currentUser = getCurrentUser(authentication); // Gọi hàm đã sửa

            // 1. Tìm Booking
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            // 2. CHECK BẢO MẬT: Người xem phải là chủ của lịch hẹn này
            if (!booking.getUser().getId().equals(currentUser.getId())) {
                ra.addFlashAttribute("errorMessage", "Bạn không có quyền xem hồ sơ bệnh án này.");
                return "redirect:/user/profile";
            }

            // 3. Tìm Hồ sơ bệnh án
            MedicalRecord record = medicalRecordRepository.findByBookingId(bookingId)
                    .orElse(null);

            if (record == null) {
                ra.addFlashAttribute("errorMessage", "Hồ sơ bệnh án chưa được cập nhật. Vui lòng liên hệ phòng khám.");
                return "redirect:/user/profile";
            }

            model.addAttribute("record", record);
            model.addAttribute("booking", booking);

            // Cấu hình giao diện User
            model.addAttribute("role", "USER");
            model.addAttribute("backLink", "/user/profile");

            return "user/medical-record-detail";

        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/user/profile";
        }
    }
}