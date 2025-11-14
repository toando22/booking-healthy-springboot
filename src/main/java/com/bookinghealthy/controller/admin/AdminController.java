package com.bookinghealthy.controller.admin;

import com.bookinghealthy.model.Role;
import com.bookinghealthy.model.User;
import com.bookinghealthy.repository.RoleRepository;
import com.bookinghealthy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException; // <-- THÊM IMPORT
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserService userService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping("/dashboard")
    public String adminHome() {
        return "admin/dashboard";
    }

    // 1. SỬA HÀM NÀY: Chỉ tìm ROLE_USER
    @GetMapping("/manage-user")
    public String manageUsers(Model model) {
        // Chỉ tìm các user là Bệnh nhân
        model.addAttribute("listUsers", userService.findByRoleName("ROLE_USER"));
        return "admin/manage-user";
    }

    // 2. HIỂN THỊ FORM THÊM MỚI (Bệnh nhân)
    @GetMapping("/manage-user/add")
    public String showAddUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("pageTitle", "Thêm mới Bệnh nhân");
        return "admin/user-form";
    }

    // 3. HIỂN THỊ FORM SỬA (Bệnh nhân)
    @GetMapping("/manage-user/edit/{id}")
    public String showEditUserForm(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {
        Optional<User> user = userService.findById(id);
        if (user.isPresent()) {
            model.addAttribute("user", user.get());
            model.addAttribute("pageTitle", "Chỉnh sửa Bệnh nhân");
            return "admin/user-form";
        } else {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy User ID: " + id);
            return "redirect:/admin/manage-user";
        }
    }

    // 4. XỬ LÝ LƯU (Bệnh nhân)
    @PostMapping("/manage-user/save")
    public String saveUser(@Valid @ModelAttribute("user") User user,
                           BindingResult bindingResult,
                           @RequestParam(name = "password", required = false) String rawPassword,
                           Model model,
                           RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", (user.getId() == null) ? "Thêm mới Bệnh nhân" : "Chỉnh sửa Bệnh nhân");
            return "admin/user-form";
        }

        try {
            if (user.getId() == null) {
                // THÊM MỚI
                if (rawPassword == null || rawPassword.isEmpty()) {
                    bindingResult.rejectValue("password", "NotBlank", "Mật khẩu là bắt buộc khi tạo mới");
                    model.addAttribute("pageTitle", "Thêm mới Bệnh nhân");
                    return "admin/user-form";
                }
                // Gán Role "USER" cho tài khoản mới
                Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
                user.setRoles(Set.of(userRole));
                user.setPassword(passwordEncoder.encode(rawPassword));

            } else {
                // CẬP NHẬT
                User existingUser = userService.findById(user.getId()).orElseThrow();
                existingUser.setFullName(user.getFullName());
                existingUser.setEmail(user.getEmail());
                existingUser.setUsername(user.getUsername());
                existingUser.setPhone(user.getPhone());

                if (rawPassword != null && !rawPassword.isEmpty()) {
                    existingUser.setPassword(passwordEncoder.encode(rawPassword));
                }
                user = existingUser;
            }

            userService.save(user);
            ra.addFlashAttribute("successMessage", "Đã lưu Bệnh nhân thành công.");
            return "redirect:/admin/manage-user";

        } catch (DataIntegrityViolationException e) { // Bắt lỗi trùng email/username
            bindingResult.rejectValue("username", "Duplicate", "Username hoặc Email đã tồn tại.");
            model.addAttribute("pageTitle", (user.getId() == null) ? "Thêm mới Bệnh nhân" : "Chỉnh sửa Bệnh nhân");
            return "admin/user-form";
        }
    }

    // 5. XỬ LÝ XÓA (Bệnh nhân)
    @GetMapping("/manage-user/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            userService.deleteById(id);
            ra.addFlashAttribute("successMessage", "Đã xóa Bệnh nhân thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi xóa Bệnh nhân (Có thể do User đã đặt lịch).");
        }
        return "redirect:/admin/manage-user";
    }
}