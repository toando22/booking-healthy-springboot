package com.bookinghealthy.repository;

import com.bookinghealthy.model.Booking;
import com.bookinghealthy.model.Doctor;
import com.bookinghealthy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Lấy lịch sử hẹn của Bệnh nhân
    List<Booking> findByUser(User user);

    // Lấy danh sách lịch hẹn của Bác sĩ
    List<Booking> findByDoctor(Doctor doctor);
}