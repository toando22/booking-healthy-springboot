package com.bookinghealthy.repository;

import com.bookinghealthy.model.Doctor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    /**
     * Sửa dòng @EntityGraph
     * Thêm "department" vào danh sách tải
     */
    @Override
    @EntityGraph(attributePaths = {"user", "user.roles", "department"}) // <-- THÊM ", department"
    List<Doctor> findAll();

    // Sửa hàm này để tải department
    @EntityGraph(attributePaths = {"user", "user.roles", "department"}) // <-- THÊM ", department"
    List<Doctor> findByDepartmentId(Long departmentId);

    List<Doctor> findByUserFullNameContainingIgnoreCase(String fullName);
}