package com.bookinghealthy.service.impl;

import com.bookinghealthy.model.Booking;
import com.bookinghealthy.model.Candidate;
import com.bookinghealthy.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async; // <-- Thêm import
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter; // <-- Thêm import

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * @Async: Chạy tác vụ gửi mail trên một luồng (thread) riêng
     * để user không phải chờ.
     * Để @Async hoạt động, bạn cần thêm @EnableAsync vào file Application chính.
     */
    @Async
    @Override
    public void sendBookingConfirmation(Booking booking) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            // Lấy email người nhận từ User (Bệnh nhân)
            message.setTo(booking.getUser().getEmail());

            // Tiêu đề Email
            message.setSubject("MediTrust - Xác nhận đặt lịch khám thành công");

            // Định dạng ngày giờ cho đẹp
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String formattedDate = booking.getAppointmentDate().format(dateFormatter);

            // Nội dung Email
            String text = String.format(
                    "Xin chào %s,\n\n" +
                            "MediTrust xác nhận bạn đã đặt lịch khám thành công.\n\n" +
                            "--- CHI TIẾT LỊCH HẸN ---\n" +
                            "Bác sĩ: Dr. %s\n" +
                            "Chuyên khoa: %s\n" +
                            "Ngày hẹn: %s\n" +
                            "Giờ hẹn: %s\n" +
                            "Loại hình khám: %s\n" +
                            "Giá khám: %s VNĐ\n\n" +
                            "Cảm ơn bạn đã tin tưởng MediTrust!",

                    booking.getUser().getFullName(),
                    booking.getDoctor().getUser().getFullName(),
                    booking.getDoctor().getDepartment().getName(),
                    formattedDate,
                    booking.getAppointmentTime(),
                    booking.getAppointmentType(),
                    booking.getBookingPrice().toString()
            );

            message.setText(text);

            // Gửi mail
            mailSender.send(message);

        } catch (Exception e) {
            // Ghi log lỗi (quan trọng cho debug)
            System.err.println("Lỗi khi gửi mail: " + e.getMessage());
        }
    }
    // === THÊM PHƯƠNG THỨC MỚI NÀY ===
    @Async
    @Override
    public void sendBookingCancellation(Booking booking, String reason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(booking.getUser().getEmail());
            message.setSubject("MediTrust - Thông báo hủy lịch hẹn");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String formattedDate = booking.getAppointmentDate().format(dateFormatter);

            String text = String.format(
                    "Xin chào %s,\n\n" +
                            "Chúng tôi rất tiếc phải thông báo lịch hẹn của bạn đã bị HỦY.\n\n" +
                            "Lý do: %s\n\n" +
                            "--- CHI TIẾT LỊCH HẸN ĐÃ HỦY ---\n" +
                            "Bác sĩ: Dr. %s\n" +
                            "Ngày hẹn: %s\n" +
                            "Giờ hẹn: %s\n\n" +
                            "Vui lòng đặt lại lịch hẹn vào một thời điểm khác. Xin cảm ơn!",

                    booking.getUser().getFullName(),
                    reason, // Hiển thị lý do (Bác sĩ từ chối, Admin hủy,...)
                    booking.getDoctor().getUser().getFullName(),
                    formattedDate,
                    booking.getAppointmentTime()
            );
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi mail HỦY: " + e.getMessage());
        }
    }
    // === 1. GỬI XÁC NHẬN CHO ỨNG VIÊN ===
    @Async
    @Override
    public void sendCandidateConfirmation(Candidate candidate) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(candidate.getEmail());
            message.setSubject("MediTrust - Xác nhận ứng tuyển: " + candidate.getJobPosting().getTitle());
            message.setText("Chào " + candidate.getFullName() + ",\n\n" +
                    "Cảm ơn bạn đã quan tâm và ứng tuyển vào vị trí " + candidate.getJobPosting().getTitle() + " tại MediTrust.\n" +
                    "Hồ sơ của bạn đã được hệ thống ghi nhận.\n\n" +
                    "Bộ phận Tuyển dụng sẽ xem xét và phản hồi lại bạn trong thời gian sớm nhất.\n\n" +
                    "Trân trọng,\nMediTrust HR Team");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail ứng viên: " + e.getMessage());
        }
    }
    // === 2. GỬI THÔNG BÁO CHO ADMIN ===
    @Async
    @Override
    public void sendNewCandidateNotification(Candidate candidate) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("doduytoan2201@gmail.com"); // Email Admin nhận tin
            message.setSubject("[HR] Ứng viên mới: " + candidate.getJobPosting().getTitle());
            message.setText("Hệ thống vừa nhận được hồ sơ mới:\n\n" +
                    "Vị trí: " + candidate.getJobPosting().getTitle() + "\n" +
                    "Ứng viên: " + candidate.getFullName() + "\n" +
                    "Email: " + candidate.getEmail() + "\n" +
                    "SĐT: " + candidate.getPhone() + "\n\n" +
                    "Vui lòng truy cập trang quản trị để xem CV chi tiết.");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail admin: " + e.getMessage());
        }
    }

    // === 3. GỬI KẾT QUẢ (DUYỆT/TỪ CHỐI) ===
    @Async
    @Override
    public void sendCandidateResult(Candidate candidate, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(candidate.getEmail());
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail kết quả: " + e.getMessage());
        }
    }
}