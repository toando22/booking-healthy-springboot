package com.bookinghealthy.controller.api;

import com.bookinghealthy.dto.DoctorDTO;
import com.bookinghealthy.model.Doctor;
import com.bookinghealthy.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DoctorApiController {

    @Autowired
    private DoctorService doctorService;

    @GetMapping("/doctors")
    public List<DoctorDTO> getDoctorsByDepartment(@RequestParam Long departmentId) {

        List<Doctor> doctors = doctorService.findByDepartmentId(departmentId);

        return doctors.stream()
                .map(DoctorDTO::new)
                .collect(Collectors.toList());
    }
}