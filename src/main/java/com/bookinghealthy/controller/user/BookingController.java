package com.bookinghealthy.controller.user;

import com.bookinghealthy.model.*;
import com.bookinghealthy.repository.DepartmentRepository;
import com.bookinghealthy.service.BookingService;
import com.bookinghealthy.service.DoctorService;
import com.bookinghealthy.service.EmailService; // (Đã có)
import com.bookinghealthy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class BookingController {

    @Autowired private DoctorService doctorService;
    @Autowired private BookingService bookingService;
    @Autowired private UserService userService;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private EmailService emailService;

    // (Hàm getCurrentUser giữ nguyên)
    private User getCurrentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String usernameOrEmail = userDetails.getUsername();
        return userService.findByUsername(usernameOrEmail)
                .or(() -> userService.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new RuntimeException("User not found with identifier: " + usernameOrEmail));
    }

    // (Hàm showAppointmentForm (GET) giữ nguyên)
    @GetMapping("/appointment")
    public String showAppointmentForm(@RequestParam(value = "doctorId", required = false) Long doctorId,
                                      Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        List<Department> departments = departmentRepository.findAll();
        model.addAttribute("departments", departments);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("selectedDoctorId", doctorId);
        return "user/appointment";
    }


    // === HÀM NÀY ĐÃ ĐƯỢC CẬP NHẬT ===
    @PostMapping("/appointment")
    public String processAppointment(
            @RequestParam("appointmentType") String appointmentType,
            @RequestParam("doctorId") Long doctorId,
            @RequestParam("appointmentDate") LocalDate appointmentDate,
            @RequestParam("appointmentTime") String appointmentTime,
            @RequestParam("notes") String notes,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        // 1. Tạo biến tạm để giữ booking
        Booking savedBooking = null;

        try {
            User currentUser = getCurrentUser(authentication);
            Doctor doctor = doctorService.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            Booking booking = new Booking();
            booking.setUser(currentUser);
            booking.setDoctor(doctor);
            booking.setAppointmentDate(appointmentDate);
            booking.setAppointmentTime(appointmentTime);
            booking.setNotes(notes);
            booking.setStatus(BookingStatus.PENDING);
            booking.setAppointmentType(appointmentType);
            booking.setBookingPrice(doctor.getPrice());

            // 2. Lưu booking và gán vào biến tạm
            savedBooking = bookingService.save(booking);

            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu đặt lịch của bạn đã được gửi thành công!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi đặt lịch: " + e.getMessage());
        }

        // 3. Gửi mail NẾU booking đã được lưu thành công
        if (savedBooking != null) {
            emailService.sendBookingConfirmation(savedBooking);
        }

        return "redirect:/appointment";
    }
}