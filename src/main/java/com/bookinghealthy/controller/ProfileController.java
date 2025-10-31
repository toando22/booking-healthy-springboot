package com.bookinghealthy.controller;

import com.bookinghealthy.dto.ChangePasswordDTO;
import com.bookinghealthy.dto.UpdateProfileDTO;
import com.bookinghealthy.model.User;
import com.bookinghealthy.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Controller
@RequestMapping("/user")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = profileService.getProfile(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("updateProfile", new UpdateProfileDTO());
        return "user/profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @ModelAttribute UpdateProfileDTO dto, Model model) {
        try {
            profileService.updateProfile(userDetails.getUsername(), dto);
            model.addAttribute("success", "Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("user", profileService.getProfile(userDetails.getUsername()));
        return "user/profile";
    }

    @PostMapping("/upload-avatar")
    public String uploadAvatar(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam("avatar") MultipartFile file,
                               Model model) {
        if (!file.isEmpty()) {
            try {
                String uploadDir = "uploads/avatar/";
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                File dest = new File(dir, fileName);
                file.transferTo(dest);
                profileService.updateAvatar(userDetails.getUsername(), "/uploads/avatar/" + fileName);
                model.addAttribute("success", "Cập nhật ảnh thành công!");
            } catch (IOException e) {
                model.addAttribute("error", "Không thể tải ảnh lên!");
            }
        }
        model.addAttribute("user", profileService.getProfile(userDetails.getUsername()));
        return "user/profile";
    }

    @GetMapping("/change-password")
    public String showChangePassword(Model model) {
        model.addAttribute("changePassword", new ChangePasswordDTO());
        return "user/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @ModelAttribute ChangePasswordDTO dto, Model model) {
        try {
            profileService.changePassword(userDetails.getUsername(), dto);
            model.addAttribute("success", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "user/change-password";
    }
}
