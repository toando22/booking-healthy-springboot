package com.bookinghealthy.service;

import com.bookinghealthy.model.Review;
import java.util.List;

public interface ReviewService {

    // Lưu đánh giá mới
    void saveReview(Long bookingId, Integer rating, String comment);

    // Lấy danh sách đánh giá của Bác sĩ
    List<Review> getReviewsByDoctor(Long doctorId);

    // Lấy thông số Rating (Trung bình sao, Tổng số lượt)
    // Trả về mảng Object[] hoặc DTO, ở đây tôi dùng DTO nội bộ đơn giản hoặc tính trực tiếp ở Controller
    Double getAverageRating(Long doctorId);

    Long countReviews(Long doctorId);
}