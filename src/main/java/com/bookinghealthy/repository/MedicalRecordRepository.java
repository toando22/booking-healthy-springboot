package com.bookinghealthy.repository;

import com.bookinghealthy.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    // Tìm hồ sơ bệnh án theo Booking ID (Để xem chi tiết)
    Optional<MedicalRecord> findByBookingId(Long bookingId);
}