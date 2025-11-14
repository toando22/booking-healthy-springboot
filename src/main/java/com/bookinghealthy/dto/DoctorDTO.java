package com.bookinghealthy.dto;

import com.bookinghealthy.model.Doctor;
import lombok.Data;
import java.math.BigDecimal; // <-- Thêm import

@Data
public class DoctorDTO {
    private Long id;
    private String fullName;
    private String departmentName;
    private String degree; // <-- Thêm
    private BigDecimal price; // <-- Thêm
    private Long departmentId; // <-- THÊM TRƯỜNG NÀY

    public DoctorDTO(Doctor doctor) {
        this.id = doctor.getId();
        this.fullName = doctor.getUser().getFullName();

        if (doctor.getDepartment() != null) {
            this.departmentName = doctor.getDepartment().getName();
            this.departmentId = doctor.getDepartment().getId(); // <-- GÁN GIÁ TRỊ
        } else {
            this.departmentName = "N/A";
            this.departmentId = null;
        }

        this.degree = doctor.getDegree(); // <-- Gán
        this.price = doctor.getPrice();   // <-- Gán
    }
}