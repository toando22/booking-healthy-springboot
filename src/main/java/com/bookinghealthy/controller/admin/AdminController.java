package com.bookinghealthy.controller.admin; // Gói của bạn có thể khác

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/dashboard")
    public String showDashboard() {
        // Trả về file: templates/admin/dashboard.html
        return "admin/dashboard";
    }

     @GetMapping("/doctors")
     public String showDoctorManagement() {
         return "doctor/dashboard"; // Sẽ cần file templates/admin/doctors.html
     }
    @GetMapping("/manageuser")
    public String showmangeuser() {
        return "admin/manage-user"; // Sẽ cần file templates/admin/doctors.html
    }
}