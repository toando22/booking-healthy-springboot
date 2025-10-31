package com.bookinghealthy.service;

import com.bookinghealthy.model.Doctor;
import java.util.List;
import java.util.Optional;

public interface DoctorService {
    List<Doctor> findAll();
    Optional<Doctor> findById(Long id);
    List<Doctor> searchBySpecialty(String specialty);
    List<Doctor> searchByName(String name);

    // (Chúng ta sẽ thêm save, update, delete khi làm Module Admin)
}