package com.bookinghealthy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết 1-1 với User.
    // Mỗi bác sĩ PHẢI là một User.
    // fetch = FetchType.LAZY: Chỉ tải thông tin User khi thực sự cần.
    // optional = false: Bắt buộc phải có 1 User.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

//    @Column(nullable = false)
//    private String specialty; // Chuyên khoa (ví dụ: Tim mạch, Nha khoa)
// === THAY THẾ BẰNG KHỐI NÀY ===
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "department_id")
private Department department; // Liên kết Bác sĩ với Khoa
    // === KẾT THÚC THAY THẾ ===
    @Column(columnDefinition = "TEXT")
    private String bio; // Tiểu sử, mô tả kinh nghiệm

    private Integer experienceYears; // Số năm kinh nghiệm

    // Chúng ta có thể thêm các trường khác sau này
    // như rating (từ Module 6: Feedback)
    // private Double rating;
}