package com.bookinghealthy.service.impl;

import com.bookinghealthy.model.Booking;
import com.bookinghealthy.model.BookingStatus;
import com.bookinghealthy.model.MedicalRecord;
import com.bookinghealthy.repository.BookingRepository;
import com.bookinghealthy.repository.MedicalRecordRepository;
import com.bookinghealthy.service.MedicalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Override
    @Transactional // Đảm bảo tính toàn vẹn dữ liệu (Lưu bệnh án + Update status phải cùng thành công)
    public void createMedicalRecord(Long bookingId, String symptoms, String diagnosis, String prescription, String doctorNotes) {

        // 1. Tìm Booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn với ID: " + bookingId));

        // 2. Kiểm tra nếu đã có bệnh án rồi thì không tạo mới (tránh duplicate)
        if (medicalRecordRepository.findByBookingId(bookingId).isPresent()) {
            throw new RuntimeException("Lịch hẹn này đã có hồ sơ bệnh án!");
        }

        // 3. Tạo MedicalRecord mới
        MedicalRecord record = new MedicalRecord();
        record.setBooking(booking);
        record.setSymptoms(symptoms);
        record.setDiagnosis(diagnosis);
        record.setPrescription(prescription);
        record.setDoctorNotes(doctorNotes);

        medicalRecordRepository.save(record);

        // 4. Cập nhật trạng thái Booking -> COMPLETED (Đã hoàn thành khám)
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
    }

    @Override
    public Optional<MedicalRecord> findByBookingId(Long bookingId) {
        return medicalRecordRepository.findByBookingId(bookingId);
    }
}