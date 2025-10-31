package com.bookinghealthy.controller.user;

import com.bookinghealthy.model.Doctor;
import com.bookinghealthy.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable; // Thêm import này
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional; // Thêm import này

@Controller
@RequestMapping("/")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    // Endpoint hiển thị danh sách bác sĩ (bạn đã có)
    @GetMapping("/doctors")
    public String showDoctorList(Model model) {
        List<Doctor> doctors = doctorService.findAll();
        model.addAttribute("doctors", doctors);
        return "user/doctors";
    }

    // === PHƯƠNG THỨC MỚI CẦN THÊM ===
    /**
     * Xử lý request xem chi tiết bác sĩ
     * URL sẽ có dạng: /doctors/1, /doctors/2, v.v.
     */
    @GetMapping("/doctors/{id}")
    public String showDoctorDetails(@PathVariable("id") Long id, Model model) {

        Optional<Doctor> doctorOpt = doctorService.findById(id);

        // Nếu không tìm thấy bác sĩ với ID này,
        // chuyển hướng người dùng về trang danh sách bác sĩ
        if (doctorOpt.isEmpty()) {
            return "redirect:/doctors";
        }

        // Nếu tìm thấy, gửi đối tượng doctor qua Model
        model.addAttribute("doctor", doctorOpt.get());

        // Trả về file HTML chi tiết
        return "user/doctor-details";
    }
}