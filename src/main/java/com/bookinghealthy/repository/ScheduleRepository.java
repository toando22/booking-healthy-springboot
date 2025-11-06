package com.bookinghealthy.repository;

import com.bookinghealthy.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // Tìm tất cả các khung giờ làm việc của 1 bác sĩ
    List<Schedule> findByDoctorId(Long doctorId);

    // Tìm khung giờ của 1 bác sĩ VÀO 1 NGÀY CỤ THỂ TRONG TUẦN
    List<Schedule> findByDoctorIdAndDayOfWeek(Long doctorId, DayOfWeek dayOfWeek);
}