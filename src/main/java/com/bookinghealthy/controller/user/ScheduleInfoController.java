package com.bookinghealthy.controller.user;

import com.bookinghealthy.model.Department;
import com.bookinghealthy.model.Doctor;
import com.bookinghealthy.model.Schedule;
import com.bookinghealthy.repository.DepartmentRepository;
import com.bookinghealthy.repository.DoctorRepository;
import com.bookinghealthy.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ScheduleInfoController {

    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private DoctorRepository doctorRepository;

    // 1. TRANG GIỜ LÀM VIỆC
    @GetMapping("/working-hours")
    public String showWorkingHours(Model model) {
        model.addAttribute("activePage", "schedule");
        return "user/working-hours";
    }

    // 2. TRANG QUY TRÌNH KHÁM
    @GetMapping("/medical-process")
    public String showMedicalProcess(Model model) {
        model.addAttribute("activePage", "schedule");
        return "user/medical-process";
    }

    // 3. TRANG LỊCH TRỰC BÁC SĨ (Logic Phức tạp)
    @GetMapping("/doctor-schedule")
    public String showDoctorSchedule(
            @RequestParam(value = "searchDate", required = false) LocalDate searchDate,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            Model model) {

        // Mặc định là hôm nay nếu không chọn
        if (searchDate == null) {
            searchDate = LocalDate.now();
        }

        // 1. Lấy Thứ của ngày đã chọn
        DayOfWeek dayOfWeek = searchDate.getDayOfWeek();

        // 2. Tìm tất cả lịch trong Thứ đó
        List<Schedule> schedules = scheduleRepository.findByDayOfWeek(dayOfWeek);

        // 3. Lọc theo Khoa (nếu chọn)
        if (departmentId != null) {
            schedules = schedules.stream()
                    .filter(s -> s.getDoctor().getDepartment().getId().equals(departmentId))
                    .collect(Collectors.toList());
        }

        // 4. Lọc theo Bác sĩ (nếu chọn)
        if (doctorId != null) {
            schedules = schedules.stream()
                    .filter(s -> s.getDoctor().getId().equals(doctorId))
                    .collect(Collectors.toList());
        }

        // 5. Gom nhóm theo Bác sĩ (Map<Doctor, List<Schedule>>)
        // Để hiển thị: 1 Hàng Bác sĩ -> Có thể có nhiều ca trực (Sáng/Chiều)
        Map<Doctor, List<Schedule>> doctorSchedulesMap = schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getDoctor));

        // Gửi dữ liệu ra View
        model.addAttribute("doctorSchedulesMap", doctorSchedulesMap);
        model.addAttribute("searchDate", searchDate); // Để hiển thị lại ngày đã chọn
        model.addAttribute("currentDepartmentId", departmentId);
        model.addAttribute("currentDoctorId", doctorId);

        // Gửi danh sách cho Dropdown bộ lọc
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("doctors", doctorRepository.findAll());

        model.addAttribute("activePage", "schedule");
        return "user/doctor-schedule";
    }
}