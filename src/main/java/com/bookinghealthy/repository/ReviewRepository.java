package com.bookinghealthy.repository;

import com.bookinghealthy.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Kiểm tra xem lịch hẹn này đã được đánh giá chưa
    Optional<Review> findByBookingId(Long bookingId);

    // Lấy danh sách đánh giá của 1 bác sĩ (để hiện lên trang chi tiết)
    // Sắp xếp mới nhất lên đầu
    @Query("SELECT r FROM Review r WHERE r.booking.doctor.id = :doctorId ORDER BY r.createdAt DESC")
    List<Review> findByDoctorId(@Param("doctorId") Long doctorId);

    // Tính điểm trung bình sao của bác sĩ (VD: 4.8)
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.booking.doctor.id = :doctorId")
    Double getAverageRating(@Param("doctorId") Long doctorId);

    // Đếm tổng số đánh giá của bác sĩ
    @Query("SELECT COUNT(r) FROM Review r WHERE r.booking.doctor.id = :doctorId")
    Long countReviews(@Param("doctorId") Long doctorId);
}