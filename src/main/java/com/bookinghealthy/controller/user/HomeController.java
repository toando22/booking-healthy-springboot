package com.bookinghealthy.controller.user;

import com.bookinghealthy.model.Department;
import com.bookinghealthy.model.Doctor;
import com.bookinghealthy.model.Service;
import com.bookinghealthy.service.DepartmentService;
import com.bookinghealthy.service.DoctorService;
import com.bookinghealthy.service.ServiceService; // Giả sử bạn đã có ServiceService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    @Autowired private DepartmentService departmentService;
    @Autowired private DoctorService doctorService;
    @Autowired private ServiceService serviceService; // Cần Service này (đã tạo ở Module 7)

    @GetMapping("/")
    public String home(Model model) {
        return showHomePage(model);
    }

    @GetMapping("/home")
    public String homeAlias(Model model) {
        return showHomePage(model);
    }

    private String showHomePage(Model model) {

        // (Tối 12/11 - update) Lấy TẤT CẢ Khoa (để dùng cho Dropdown tìm kiếm)
        List<Department> allDepartments = departmentService.findAll();

        // 1. Lấy 6 Khoa đầu tiên
        List<Department> departments = departmentService.findAll().stream()
                .limit(6)
                .collect(Collectors.toList());

        // 2. Lấy 6 Bác sĩ đầu tiên
        List<Doctor> doctors = doctorService.findAll().stream()
                .limit(6)
                .collect(Collectors.toList());

        // 3. Lấy 4 Dịch vụ đầu tiên
        List<Service> services = serviceService.findAll().stream()
                .limit(4)
                .collect(Collectors.toList());
        // Gửi dữ liệu ra View
        model.addAttribute("allDepartments", allDepartments); // <-- DÙNG CÁI NÀY CHO DROPDOWN
        model.addAttribute("featuredDepartments", departments);
        model.addAttribute("featuredDoctors", doctors);
        model.addAttribute("featuredServices", services);

        return "user/index"; // Trỏ đến file templates/user/index.html
    }
}