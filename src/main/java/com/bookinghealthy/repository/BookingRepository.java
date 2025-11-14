package com.bookinghealthy.repository;

import com.bookinghealthy.model.Booking;
import com.bookinghealthy.model.BookingStatus;
import com.bookinghealthy.model.Doctor;
import com.bookinghealthy.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Lấy lịch sử hẹn của Bệnh nhân
    @EntityGraph(attributePaths = {"doctor", "doctor.user", "doctor.department"})
    List<Booking> findByUser(User user);

    // === ĐÃ XÓA @Override GÂY LỖI ===
    // Thêm @EntityGraph để Doctor Dashboard tải đủ thông tin
    @EntityGraph(attributePaths = {"user", "doctor", "doctor.user", "doctor.department"})
    List<Booking> findByDoctor(Doctor doctor);

    // (Hàm findAll() giữ nguyên @Override - vì nó đúng)
    @Override
    @EntityGraph(attributePaths = {
            "user",
            "doctor",
            "doctor.user",
            "doctor.department"
    })
    List<Booking> findAll();

    // (Hàm findById() giữ nguyên @Override - vì nó đúng)
    @Override
    @EntityGraph(attributePaths = {
            "user",
            "doctor",
            "doctor.user",
            "doctor.department"
    })
    Optional<Booking> findById(Long id);
//    // === THÊM HÀM MỚI NÀY ===
//    /**
//     * Lấy 5 lịch hẹn mới nhất (dựa trên thời gian tạo)
//     * Tải tất cả thông tin liên quan để hiển thị trên Dashboard
//     */
//    @EntityGraph(attributePaths = {"user", "doctor", "doctor.user"})
//    List<Booking> findFirst5ByOrderByCreatedAtDesc();
     @EntityGraph(attributePaths = {"user", "doctor", "doctor.user"})
     List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);
    // === THÊM HÀM MỚI NÀY ===
    /**
     * Lấy TẤT CẢ lịch hẹn, sắp xếp mới nhất lên đầu
     * Tải tất cả thông tin liên quan để hiển thị trên Dashboard
     */
    @EntityGraph(attributePaths = {"user", "doctor", "doctor.user"})
    List<Booking> findAllByOrderByCreatedAtDesc();

    // === THÊM HÀM MỚI NÀY ===
    /**
     * Lấy dữ liệu thống kê cho Biểu đồ Dashboard.
     * Đếm số lượng lịch hẹn (bookings) theo từng ngày (DATE(created_at))
     * trong 7 ngày qua (CURDATE() - INTERVAL 7 DAY)
     * (Đây là một câu Native Query SQL)
     */
    @Query(value = "SELECT DATE(created_at) as date, COUNT(id) as count " +
            "FROM bookings " +
            "WHERE created_at >= CURDATE() - INTERVAL 7 DAY " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY date ASC", nativeQuery = true)
    List<Object[]> getBookingStatsForLast7Days();

    // === THÊM HÀM MỚI NÀY ===
    // Đếm số lượng lịch hẹn theo Khoa và Trạng thái (ví dụ: COMPLETED)
    long countByDoctor_Department_IdAndStatus(Long departmentId, BookingStatus status);
}