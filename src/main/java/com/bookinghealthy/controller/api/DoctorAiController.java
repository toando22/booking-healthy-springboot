package com.bookinghealthy.controller.api;

import com.bookinghealthy.dto.ai.ChatRequest;
import com.bookinghealthy.model.Booking;
import com.bookinghealthy.model.BookingStatus;
import com.bookinghealthy.model.Doctor;
import com.bookinghealthy.model.User;
import com.bookinghealthy.repository.BookingRepository;
import com.bookinghealthy.repository.DoctorRepository;
import com.bookinghealthy.repository.UserRepository;
import com.bookinghealthy.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import com.bookinghealthy.model.Review;
import com.bookinghealthy.repository.ReviewRepository;
import com.bookinghealthy.service.ReviewService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/doctor/chat")
public class DoctorAiController {

    @Autowired
    private AiService aiService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewService reviewService;
    private Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return userRepository.findByUsername(username);
        } else if (principal instanceof OAuth2User) {
            String email = ((OAuth2User) principal).getAttribute("email");
            if (email != null) return userRepository.findByEmail(email);
        } else {
            String name = auth.getName();
            Optional<User> userOpt = userRepository.findByUsername(name);
            if (userOpt.isEmpty()) userOpt = userRepository.findByEmail(name);
            return userOpt;
        }
        return Optional.empty();
    }

    private Optional<Doctor> getCurrentDoctor() {
        return getCurrentUser().flatMap(user -> doctorRepository.findByUserId(user.getId()));
    }

    @GetMapping("/welcome")
    public ResponseEntity<String> getWelcomeMessage() {
        Optional<Doctor> doctorOpt = getCurrentDoctor();
        if (doctorOpt.isEmpty()) {
            return ResponseEntity.ok("Xin chào Bác sĩ! Em là trợ lý AI thống kê lịch khám. Em có thể giúp gì cho bác sĩ hôm nay?");
        }
        Doctor doctor = doctorOpt.get();
        String doctorName = doctor.getUser().getFullName();

        LocalDate today = LocalDate.now();
        long todayTotalNoCancel = bookingRepository.countByDoctorIdAndStatusNotAndDateRange(doctor.getId(), BookingStatus.CANCELED, today, today);
        long todayCompleted = bookingRepository.countByDoctorIdAndStatusAndDateRange(doctor.getId(), BookingStatus.COMPLETED, today, today);
        long todayRemaining = Math.max(0, todayTotalNoCancel - todayCompleted);
        long incompleteRecords = bookingRepository.countIncompleteRecordsByDoctor(doctor.getId(), BookingStatus.COMPLETED, today);

        String message = String.format("Chào Bác sĩ **%s**! Hôm nay bác sĩ có **%d** lịch hẹn. Đã khám **%d**, còn lại **%d**. ",
            doctorName, todayTotalNoCancel, todayCompleted, todayRemaining);

        if (incompleteRecords > 0) {
            message += String.format("🚨 Bác sĩ lưu ý đang có **%d** hồ sơ bệnh án khám xong nhưng chưa được điền đủ thông tin/chưa hoàn thành nhé!", incompleteRecords);
        } else {
            message += "Tất cả hồ sơ bệnh án của các ca đã khám đều được hoàn thành tốt ạ!";
        }

        return ResponseEntity.ok(message);
    }
    @GetMapping("/quick-review-advice")
    public ResponseEntity<Map<String, String>> getQuickReviewAdvice() {
        Optional<Doctor> doctorOpt = getCurrentDoctor();
        if (doctorOpt.isEmpty()) {
            return ResponseEntity.status(403).build();
        }

        Doctor currentDoctor = doctorOpt.get();
        Double avgRating = reviewService.getAverageRating(currentDoctor.getId());

        String advice = "Chưa có đủ đánh giá.";
        String colorClass = "text-muted";

        if (avgRating != null) {
            if (avgRating >= 4.5) {
                advice = "Tuyệt vời! Hãy tiếp tục duy trì thái độ tích cực nhé.";
                colorClass = "text-success fw-bold";
            } else if (avgRating >= 3.5) {
                advice = "Tốt! Nhưng có vài điểm nhỏ cần cải thiện để đạt 5 sao.";
                colorClass = "text-warning text-dark fw-bold";
            } else {
                advice = "Cảnh báo! Điểm đánh giá đang thấp, cần khắc phục ngay.";
                colorClass = "text-danger fw-bold";
            }
        }

        Map<String, String> response = new HashMap<>();
        response.put("advice", advice);
        response.put("colorClass", colorClass);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askAi(@RequestBody ChatRequest request) {
        Optional<Doctor> doctorOpt = getCurrentDoctor();
        if (doctorOpt.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("answer", "Vui lòng đăng nhập với tư cách Bác sĩ."));
        }

        Doctor doctor = doctorOpt.get();

        // 1. Chuẩn bị các mốc thời gian
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate startOfWeek = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(java.time.DayOfWeek.SUNDAY);
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        // 2. Lấy số liệu thống kê
        long todayActiveTotal = bookingRepository.countByDoctorIdAndStatusNotAndDateRange(doctor.getId(), BookingStatus.CANCELED, today, today);
        long todayCompleted = bookingRepository.countByDoctorIdAndStatusAndDateRange(doctor.getId(), BookingStatus.COMPLETED, today, today);
        long todayRemaining = Math.max(0, todayActiveTotal - todayCompleted);
        long todayTotal = bookingRepository.countByDoctorIdAndDateRange(doctor.getId(), today, today);
        long weekTotal = bookingRepository.countByDoctorIdAndDateRange(doctor.getId(), startOfWeek, endOfWeek);
        long weekCompleted = bookingRepository.countByDoctorIdAndStatusAndDateRange(doctor.getId(), BookingStatus.COMPLETED, startOfWeek, endOfWeek);
        long weekRemaining = Math.max(0, weekTotal - weekCompleted);
        long monthTotal = bookingRepository.countByDoctorIdAndDateRange(doctor.getId(), startOfMonth, endOfMonth);
        long monthCompleted = bookingRepository.countByDoctorIdAndStatusAndDateRange(doctor.getId(), BookingStatus.COMPLETED, startOfMonth, endOfMonth);
        long incompleteRecords = bookingRepository.countIncompleteRecordsByDoctor(doctor.getId(), BookingStatus.COMPLETED, today);

        // 3. Lấy chi tiết lịch khám
        List<Booking> upcomingBookings = bookingRepository.findDetailedBookingsForAi(doctor.getId(), today, tomorrow);
        StringBuilder scheduleDetails = new StringBuilder();

        if (upcomingBookings.isEmpty()) {
            scheduleDetails.append("Không có lịch khám nào được đặt trong hôm nay và ngày mai.\n");
        } else {
            for (Booking b : upcomingBookings) {
                String patientName = b.getUser() != null ? b.getUser().getFullName() : "Khách vãng lai";
                scheduleDetails.append(String.format("- Ngày %s | Giờ: %s | Bệnh nhân: %s | Trạng thái: %s\n",
                        b.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        b.getAppointmentTime(),
                        patientName,
                        b.getStatus()));
            }
        }

        // 4. LẤY CHI TIẾT ĐÁNH GIÁ (BÌNH LUẬN) ĐỂ PHÂN TÍCH
        List<Review> recentReviews = reviewRepository.findByDoctorId(doctor.getId());
        StringBuilder reviewTexts = new StringBuilder();
        int reviewCount = 0;

        for (Review r : recentReviews) {
            // Chỉ lấy các review có viết bình luận chữ
            if (r.getComment() != null && !r.getComment().trim().isEmpty()) {
                reviewTexts.append("- ").append(r.getRating()).append(" sao: ").append(r.getComment()).append("\n");
                reviewCount++;
                if (reviewCount >= 10) break; // Giới hạn lấy 10 bình luận gần nhất để tránh tràn Token của AI
            }
        }
        if (reviewCount == 0) {
            reviewTexts.append("Chưa có đánh giá nào bằng chữ gần đây.\n");
        }

        // 5. Build Context Message cho AI
        String contextMsg = String.format("Bạn là AI Trợ lý Bác sĩ hệ thống MediTrust. Nhiệm vụ của bạn là hỗ trợ bác sĩ. Phải trả lời ngắn gọn, tự nhiên, xưng hô 'Em' với 'Bác sĩ'.\n\n"
                        + "--- THỐNG KÊ TỔNG QUAN ---\n"
                        + "- HÔM NAY (%s): Tổng %d ca, đã khám %d ca, còn %d ca chưa khám (Tổng tất cả bao gồm hủy: %d ca)\n"
                        + "- TUẦN NÀY (%s đến %s): Tổng %d ca, đã khám %d ca, còn %d ca\n"
                        + "- THÁNG NÀY (%s): Tổng %d ca, đã khám %d ca\n"
                        + "- HỒ SƠ BỆNH ÁN CÒN NỢ: %d hồ sơ.\n\n"
                        + "--- CHI TIẾT LỊCH KHÁM HÔM NAY VÀ NGÀY MAI ---\n"
                        + "%s\n"
                        + "--- ĐÁNH GIÁ CỦA BỆNH NHÂN GẦN ĐÂY ---\n"
                        + "%s\n"
                        + "--- QUY TẮC ---\n"
                        + "1. TÌM LỊCH TRỐNG: Giờ làm việc là Sáng 07:30-12:00, Chiều 13:30-17:30, Tối 17:30-19:30 (Mỗi ca 30p). Nếu bác sĩ hỏi giờ trống tiếp theo, hãy nhìn vào giờ hiện tại (Bây giờ là: %s) và danh sách lịch khám để tìm khung giờ trống gần nhất.\n"
                        + "2. PHÂN TÍCH ĐÁNH GIÁ: Nếu bác sĩ yêu cầu phân tích đánh giá, hãy đọc danh sách đánh giá trên và chỉ ra rõ 🟢 Điểm phát huy và 🔴 Điểm cần cải thiện dựa trên các bình luận.\n\n"
                        + "Người dùng hỏi: %s",
                today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), todayActiveTotal, todayCompleted, todayRemaining, todayTotal,
                startOfWeek.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), endOfWeek.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), weekTotal, weekCompleted, weekRemaining,
                startOfMonth.format(DateTimeFormatter.ofPattern("MM/yyyy")), monthTotal, monthCompleted,
                incompleteRecords,
                scheduleDetails.toString(),
                reviewTexts.toString(),
                java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                request.getPrompt()
        );

        // 6. Gọi AI Service
        String answer = aiService.chatWithMemory(request.getSessionId() + "_doctor", contextMsg);

        Map<String, String> result = new HashMap<>();
        result.put("answer", answer);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/clear/{sessionId}")
    public ResponseEntity<String> clearChat(@PathVariable String sessionId) {
        aiService.clearMemory(sessionId + "_doctor");
        return ResponseEntity.ok("Đã xóa lịch sử chat của phiên: " + sessionId);
    }
}
