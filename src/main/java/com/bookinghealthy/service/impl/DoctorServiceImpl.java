package com.bookinghealthy.service.impl;

import com.bookinghealthy.model.Doctor;
import com.bookinghealthy.repository.DoctorRepository;
import com.bookinghealthy.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DoctorServiceImpl implements DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Override
    public List<Doctor> findAll() {
        return doctorRepository.findAll();
    }

    @Override
    public Optional<Doctor> findById(Long id) {
        return doctorRepository.findById(id);
    }

    // === SỬA HÀM NÀY ===
    @Override
    public List<Doctor> findByDepartmentId(Long departmentId) {
        // Gọi hàm mới trong repository
        return doctorRepository.findByDepartmentId(departmentId);
    }
    // === KẾT THÚC SỬA ĐỔI ===

    @Override
    public List<Doctor> searchByName(String name) {
        return doctorRepository.findByUserFullNameContainingIgnoreCase(name);
    }
    // === THÊM 2 PHƯƠNG THỨC MỚI NÀY ===
    @Override
    public Doctor save(Doctor doctor) {
        return doctorRepository.save(doctor);
    }

    @Override
    public void deleteById(Long id) {
        doctorRepository.deleteById(id);
    }

    // === THÊM PHƯƠNG THỨC MỚI NÀY ===
    @Override
    public Optional<Doctor> findByUsername(String username) {
        return doctorRepository.findByUser_Username(username);
    }
}