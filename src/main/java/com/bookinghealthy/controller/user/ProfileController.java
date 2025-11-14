package com.bookinghealthy.controller.user;

import com.bookinghealthy.dto.UpdateProfileDTO;
import com.bookinghealthy.model.Booking;
import com.bookinghealthy.model.BookingStatus;
import com.bookinghealthy.model.User;
import com.bookinghealthy.service.BookingService;
import com.bookinghealthy.service.EmailService;
import com.bookinghealthy.service.ProfileService;
import com.bookinghealthy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/user") // Tất cả link bắt đầu bằng /user
public class ProfileController {

    @Autowired private ProfileService profileService;
    @Autowired private BookingService bookingService;
    @Autowired private UserService userService;
    @Autowired private EmailService emailService;

    // --- 1. TRANG HỒ SƠ (Xem & Lịch sử) ---
    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();
        User user = profileService.getProfile(username);

        // DTO để cập nhật
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());

        // Lấy lịch sử khám
        List<Booking> myBookings = bookingService.findByUser(user);

        model.addAttribute("user", user);
        model.addAttribute("updateProfile", dto);
        model.addAttribute("myBookings", myBookings);

        return "user/profile";
    }

    // --- 2. CẬP NHẬT THÔNG TIN ---
    @PostMapping("/update-profile")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @ModelAttribute UpdateProfileDTO dto,
                                RedirectAttributes ra) {
        try {
            profileService.updateProfile(userDetails.getUsername(), dto);
            ra.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/user/profile";
    }

    // --- 3. ĐỔI ẢNH ĐẠI DIỆN ---
    @PostMapping("/upload-avatar")
    public String uploadAvatar(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam("avatar") MultipartFile file,
                               RedirectAttributes ra) {
        if (!file.isEmpty()) {
            try {
                // Lưu vào thư mục static/uploads trong project
                String folderPath = "src/main/resources/static/uploads/";
                File dir = new File(folderPath);
                if (!dir.exists()) dir.mkdirs();

                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path path = Paths.get(folderPath + fileName);
                Files.write(path, file.getBytes());

                // Lưu tên file vào DB
                profileService.updateAvatar(userDetails.getUsername(), fileName);

                ra.addFlashAttribute("successMessage", "Cập nhật ảnh thành công!");
            } catch (IOException e) {
                ra.addFlashAttribute("errorMessage", "Lỗi tải ảnh: " + e.getMessage());
            }
        }
        return "redirect:/user/profile";
    }

    // --- 4. TRANG ĐỔI MẬT KHẨU (GET) ---
    @GetMapping("/change-password")
    public String showChangePasswordForm() {
        return "user/change-password"; // Trả về file change-password.html
    }

    // --- 5. XỬ LÝ ĐỔI MẬT KHẨU (POST) ---
    @PostMapping("/change-password")
    public String processChangePassword(@RequestParam("currentPassword") String currentPassword,
                                        @RequestParam("newPassword") String newPassword,
                                        @RequestParam("confirmPassword") String confirmPassword,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        RedirectAttributes ra) {

        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("errorMessage", "Mật khẩu xác nhận không khớp!");
            return "redirect:/user/change-password";
        }

        try {
            profileService.changePassword(userDetails.getUsername(), currentPassword, newPassword);
            ra.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
            return "redirect:/user/profile";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/user/change-password";
        }
    }

    // --- 6. HỦY LỊCH HẸN ---
    @GetMapping("/cancel-booking/{id}")
    public String cancelMyBooking(@PathVariable("id") Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes ra) {
        try {
            // Lấy user từ DB để so sánh ID an toàn
            User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();
            Booking booking = bookingService.findById(id).orElseThrow(() -> new Exception("Không tìm thấy lịch"));

            if (!booking.getUser().getId().equals(currentUser.getId())) {
                ra.addFlashAttribute("errorMessage", "Bạn không có quyền hủy lịch này.");
                return "redirect:/user/profile";
            }

            if (booking.getStatus() == BookingStatus.PENDING) {
                booking.setStatus(BookingStatus.CANCELED);
                bookingService.save(booking);
                // Gửi mail
                emailService.sendBookingCancellation(booking, "Người bệnh tự hủy.");
                ra.addFlashAttribute("successMessage", "Đã hủy lịch hẹn.");
            } else {
                ra.addFlashAttribute("errorMessage", "Không thể hủy lịch này.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/user/profile";
    }
}