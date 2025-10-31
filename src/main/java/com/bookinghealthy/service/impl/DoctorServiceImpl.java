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

    @Override
    public List<Doctor> searchBySpecialty(String specialty) {
        return doctorRepository.findBySpecialtyContainingIgnoreCase(specialty);
    }

    @Override
    public List<Doctor> searchByName(String name) {
        return doctorRepository.findByUserFullNameContainingIgnoreCase(name);
    }
}