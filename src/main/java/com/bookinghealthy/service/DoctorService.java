package com.bookinghealthy.service;

import com.bookinghealthy.model.Doctor;
import java.util.List;
import java.util.Optional;

public interface DoctorService {
    List<Doctor> findAll();
    Optional<Doctor> findById(Long id);
//    List<Doctor> searchBySpecialty(String specialty);
// === THAY BẰNG DÒNG NÀY ===
    List<Doctor> findByDepartmentId(Long departmentId);
    List<Doctor> searchByName(String name);

    // (Chúng ta sẽ thêm save, update, delete khi làm Module Admin)
    // === THÊM 2 HÀM MỚI ===
    Doctor save(Doctor doctor);
    void deleteById(Long id);

    // === THÊM HÀM MỚI NÀY ===
    Optional<Doctor> findByUsername(String username);
}