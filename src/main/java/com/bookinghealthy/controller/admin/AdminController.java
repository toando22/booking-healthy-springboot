package com.bookinghealthy.controller.admin;

import com.bookinghealthy.model.Booking;
import com.bookinghealthy.model.Role;
import com.bookinghealthy.model.User;
import com.bookinghealthy.repository.BookingRepository;
import com.bookinghealthy.repository.RoleRepository;
import com.bookinghealthy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.security.core.Authentication; // <-- THÊM IMPORT NÀY
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet; // <-- THÊM IMPORT
import java.util.List;
import java.util.Optional;
import java.util.Set; // <-- THÊM IMPORT

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserService userService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // === THÊM REPO NÀY ĐỂ ĐẾM ===
    @Autowired
    private BookingRepository bookingRepository;

    // === SỬA HÀM NÀY ĐỂ GỬI SỐ LIỆU ===
    @GetMapping("/dashboard")
    public String adminHome(Model model) {
        // Đếm
        long patientCount = userService.findByRoleName("ROLE_USER").size();
        long doctorCount = userService.findByRoleName("ROLE_DOCTOR").size();
        long bookingCount = bookingRepository.count(); // Đếm tổng số lịch hẹn
//        // 2. Lấy 5 Lịch hẹn gần đây (MỚI)
//        List<Booking> recentBookings = bookingRepository.findFirst5ByOrderByCreatedAtDesc();
        // 2. Lấy TẤT CẢ Lịch hẹn (Sắp xếp mới nhất)
        List<Booking> allRecentBookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        // Gửi số liệu ra view
        model.addAttribute("patientCount", patientCount);
        model.addAttribute("doctorCount", doctorCount);
        model.addAttribute("bookingCount", bookingCount);
        model.addAttribute("listBookings", allRecentBookings); // <-- GỬI DỮ LIỆU MỚI
        return "admin/dashboard";
    }

    // (Các hàm manageUsers, showAddUserForm, showEditUserForm, saveUser, deleteUser giữ nguyên)
    // ...

    // 1. SỬA HÀM NÀY: Dùng findAll() để lấy TẤT CẢ user
    @GetMapping("/manage-user")
    public String manageUsers(Model model) {
        model.addAttribute("listUsers", userService.findAll());
        return "admin/manage-user";
    }

    // 2. SỬA HÀM NÀY: Gửi danh sách Role
    @GetMapping("/manage-user/add")
    public String showAddUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("allRoles", roleRepository.findAll()); // Gửi Roles ra form
        model.addAttribute("pageTitle", "Thêm mới Người dùng");
        return "admin/user-form";
    }

    // 3. SỬA HÀM NÀY: Gửi danh sách Role
    @GetMapping("/manage-user/edit/{id}")
    public String showEditUserForm(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {
        Optional<User> user = userService.findById(id);
        if (user.isPresent()) {
            model.addAttribute("user", user.get());
            model.addAttribute("allRoles", roleRepository.findAll()); // Gửi Roles ra form
            model.addAttribute("pageTitle", "Chỉnh sửa Người dùng");
            return "admin/user-form";
        } else {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy User ID: " + id);
            return "redirect:/admin/manage-user";
        }
    }

    // 4. XỬ LÝ LƯU (NÂNG CẤP LỚN)
    @PostMapping("/manage-user/save")
    public String saveUser(@Valid @ModelAttribute("user") User user,
                           BindingResult bindingResult,
                           @RequestParam(name = "password", required = false) String rawPassword,
                           @RequestParam(name = "roleIds", required = false) Set<Long> roleIds, // Lấy Role IDs
                           Model model,
                           RedirectAttributes ra) {

        // Bắt lỗi Validation (Email, NotBlank...)
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", (user.getId() == null) ? "Thêm mới Người dùng" : "Chỉnh sửa Người dùng");
            model.addAttribute("allRoles", roleRepository.findAll());
            return "admin/user-form";
        }

        // Lấy Set<Role> từ Set<Long>
        Set<Role> roles = new HashSet<>();
        if (roleIds != null) {
            roles.addAll(roleRepository.findAllById(roleIds));
        }

        try {
            if (user.getId() == null) {
                // A. TRƯỜNG HỢP THÊM MỚI
                if (rawPassword == null || rawPassword.isEmpty()) {
                    bindingResult.rejectValue("password", "NotBlank", "Mật khẩu là bắt buộc khi tạo mới");
                    model.addAttribute("pageTitle", "Thêm mới Người dùng");
                    model.addAttribute("allRoles", roleRepository.findAll());
                    return "admin/user-form";
                }
                user.setRoles(roles); // Gán vai trò
                user.setPassword(passwordEncoder.encode(rawPassword));

            } else {
                // B. TRƯỜNG HỢP CẬP NHẬT
                User existingUser = userService.findById(user.getId()).orElseThrow();
                existingUser.setFullName(user.getFullName());
                existingUser.setEmail(user.getEmail());
                existingUser.setUsername(user.getUsername());
                existingUser.setPhone(user.getPhone());
                existingUser.setRoles(roles); // Cập nhật vai trò

                // Chỉ cập nhật mật khẩu NẾU admin nhập mật khẩu mới
                if (rawPassword != null && !rawPassword.isEmpty()) {
                    existingUser.setPassword(passwordEncoder.encode(rawPassword));
                }
                user = existingUser;
            }

            userService.save(user);
            ra.addFlashAttribute("successMessage", "Đã lưu Người dùng thành công.");
            return "redirect:/admin/manage-user";

        } catch (DataIntegrityViolationException e) {
            bindingResult.rejectValue("username", "Duplicate", "Username hoặc Email đã tồn tại.");
            model.addAttribute("pageTitle", (user.getId() == null) ? "Thêm mới Người dùng" : "Chỉnh sửa Người dùng");
            model.addAttribute("allRoles", roleRepository.findAll());
            return "admin/user-form";
        }
    }

    // 5. XỬ LÝ XÓA (ĐÃ THÊM NGHIỆP VỤ)
    @GetMapping("/manage-user/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes ra, Authentication authentication) {

        // Nghiệp vụ 1: Không cho Admin tự xóa mình
        String currentAdminUsername = authentication.getName();
        User userToDelete = userService.findById(id).orElse(null);

        if (userToDelete != null && userToDelete.getUsername().equals(currentAdminUsername)) {
            ra.addFlashAttribute("errorMessage", "Không thể xóa tài khoản Admin đang đăng nhập!");
            return "redirect:/admin/manage-user";
        }

        try {
            userService.deleteById(id);
            ra.addFlashAttribute("successMessage", "Đã xóa Người dùng thành công.");
        } catch (Exception e) {
            // Nghiệp vụ 2: Bắt lỗi Khóa ngoại (đã có lịch hẹn,...)
            ra.addFlashAttribute("errorMessage", "Không thể xóa User (đã có lịch hẹn hoặc liên kết Bác sĩ).");
        }
        return "redirect:/admin/manage-user";
    }
}