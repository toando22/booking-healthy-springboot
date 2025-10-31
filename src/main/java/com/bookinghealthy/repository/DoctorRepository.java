package com.bookinghealthy.repository;

import com.bookinghealthy.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // Đảm bảo bạn đã import java.util.List

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // === THÊM DÒNG NÀY ===
    // Tìm bác sĩ theo chuyên khoa
    List<Doctor> findBySpecialtyContainingIgnoreCase(String specialty);

    // === VÀ THÊM DÒNG NÀY ===
    // Tìm bác sĩ dựa trên tên (từ bảng User)
    List<Doctor> findByUserFullNameContainingIgnoreCase(String fullName);
}