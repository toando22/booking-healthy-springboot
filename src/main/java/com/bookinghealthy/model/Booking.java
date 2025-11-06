package com.bookinghealthy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết với Bệnh nhân (User)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Liên kết với Bác sĩ (Doctor)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    // Liên kết với Dịch vụ (Service)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(nullable = false)
    private LocalDate appointmentDate; // Ngày hẹn

    @Column(nullable = false)
    private String appointmentTime; // Giờ hẹn (ví dụ: "09:00 AM - 10:00 AM")

    @Column(columnDefinition = "TEXT")
    private String notes; // Ghi chú của bệnh nhân

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status; // Trạng thái lịch hẹn

    @CreationTimestamp // Tự động gán ngày giờ tạo
    @Column(updatable = false)
    private LocalDateTime createdAt;
}