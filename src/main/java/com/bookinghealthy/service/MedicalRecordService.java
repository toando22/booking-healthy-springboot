package com.bookinghealthy.service;

import com.bookinghealthy.model.MedicalRecord;

import java.util.Optional;

public interface MedicalRecordService {

    // Lưu bệnh án mới (Đồng thời set Booking thành COMPLETED)
    void createMedicalRecord(Long bookingId, String symptoms, String diagnosis, String prescription, String doctorNotes);

    // Tìm kiếm bệnh án theo ID lịch hẹn
    Optional<MedicalRecord> findByBookingId(Long bookingId);
}