package com.bookinghealthy.controller.user;

import com.bookinghealthy.model.*;
import com.bookinghealthy.service.BookingService;
import com.bookinghealthy.service.DoctorService;
import com.bookinghealthy.service.ServiceService;
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
import java.util.Optional;

@Controller
public class BookingController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    /**
     * === HÀM TRỢ GIÚP ĐÃ ĐƯỢC CẬP NHẬT ===
     * Tìm user hiện tại, bất kể Spring Security lưu username hay email
     */
    private User getCurrentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String usernameOrEmail = userDetails.getUsername();

        // Thử tìm bằng username, nếu không thấy, thử tìm bằng email
        return userService.findByUsername(usernameOrEmail)
                .or(() -> userService.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new RuntimeException("User not found with identifier: " + usernameOrEmail));
    }

    /**
     * Hiển thị form đặt lịch
     */
    @GetMapping("/appointment")
    public String showAppointmentForm(@RequestParam(value = "doctorId", required = false) Long doctorId,
                                      Model model, Authentication authentication) {

        // Lấy thông tin user đang đăng nhập (SỬ DỤNG HÀM MỚI)
        User currentUser = getCurrentUser(authentication);

        // Tải danh sách bác sĩ và dịch vụ
        List<Doctor> doctors = doctorService.findAll();
        List<Service> services = serviceService.findAll();

        // Gửi data ra view
        model.addAttribute("doctors", doctors);
        model.addAttribute("services", services);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("selectedDoctorId", doctorId); // Gửi doctorId (nếu có)

        return "user/appointment";
    }

    /**
     * Xử lý POST request khi user nhấn nút "Book Appointment"
     */
    @PostMapping("/appointment")
    public String processAppointment(@RequestParam("serviceId") Long serviceId,
                                     @RequestParam("doctorId") Long doctorId,
                                     @RequestParam("appointmentDate") LocalDate appointmentDate,
                                     @RequestParam("appointmentTime") String appointmentTime,
                                     @RequestParam("notes") String notes,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            // 1. Lấy User hiện tại (SỬ DỤNG HÀM MỚI)
            User currentUser = getCurrentUser(authentication);

            // 2. Lấy Doctor và Service từ ID
            Doctor doctor = doctorService.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));
            Service service = serviceService.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            // 3. Tạo đối tượng Booking mới
            Booking booking = new Booking();
            booking.setUser(currentUser);
            booking.setDoctor(doctor);
            booking.setService(service);
            booking.setAppointmentDate(appointmentDate);
            booking.setAppointmentTime(appointmentTime);
            booking.setNotes(notes);
            booking.setStatus(BookingStatus.PENDING); // Trạng thái ban đầu: Chờ xác nhận

            // 4. Lưu vào database
            bookingService.save(booking);

            // 5. Gửi thông báo thành công
            redirectAttributes.addFlashAttribute("successMessage", "Your appointment request has been sent successfully. We will contact you shortly!");

        } catch (Exception e) {
            // 6. Gửi thông báo lỗi
            redirectAttributes.addFlashAttribute("errorMessage", "Error booking appointment: " + e.getMessage());
            e.printStackTrace(); // In lỗi ra console để debug
        }

        return "redirect:/appointment"; // Chuyển hướng về trang đặt lịch
    }
}