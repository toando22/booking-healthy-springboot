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
import org.springframework.http.HttpHeaders;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/doctor/chat")
public class DoctorAiController {

    @Autowired private AiService aiService;
    @Autowired private UserRepository userRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private ReviewService reviewService;

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

        // Bọc thẻ span có ID để Frontend dễ dàng dùng JS ghi đè dữ liệu mới vào
        String message = String.format("<span id='live-welcome-stats'>Chào Bác sĩ **%s**! Hôm nay bác sĩ có **%d** lịch hẹn. Đã khám **%d**, còn lại **%d**. ",
                doctorName, todayTotalNoCancel, todayCompleted, todayRemaining);

        if (incompleteRecords > 0) {
            message += String.format("🚨 Bác sĩ lưu ý đang có **%d** hồ sơ bệnh án khám xong nhưng chưa hoàn thành nhé!</span>", incompleteRecords);
        } else {
            message += "Tất cả hồ sơ bệnh án của các ca đã khám đều được hoàn thành tốt ạ!</span>";
        }

        // Trả về kèm Header chống lưu cache để đảm bảo luôn lấy số mới nhất
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(message);
    }

    @GetMapping("/quick-review-advice")
    public ResponseEntity<Map<String, String>> getQuickReviewAdvice() {
        Optional<Doctor> doctorOpt = getCurrentDoctor();
        if (doctorOpt.isEmpty()) return ResponseEntity.status(403).build();

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

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate startOfWeek = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(java.time.DayOfWeek.SUNDAY);
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

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

        List<Booking> upcomingBookings = bookingRepository.findDetailedBookingsForAi(doctor.getId(), today, tomorrow);
        StringBuilder scheduleDetails = new StringBuilder();

        // 1. Logic tìm CA KHÁM (BOOKED) và LỊCH TRỐNG (EMPTY)
        String nextAppointmentStr = "Hiện tại không còn ca khám nào chờ khám.";
        String nextEmptySlotStr = "Không còn lịch trống trong hôm nay và ngày mai.";

        java.time.LocalTime nowTime = java.time.LocalTime.now();
        boolean foundNextBooked = false;
        boolean foundNextEmpty = false;

        Set<String> bookedToday = new HashSet<>();
        Set<String> bookedTomorrow = new HashSet<>();

        if (upcomingBookings.isEmpty()) {
            scheduleDetails.append("Không có lịch khám nào.\n");
        } else {
            for (Booking b : upcomingBookings) {
                String patientName = b.getUser() != null ? b.getUser().getFullName() : "Khách vãng lai";
                String fullTimeStr = b.getAppointmentTime();
                String startTimeStr = fullTimeStr.split("-")[0].trim();

                java.time.LocalTime appointmentLocalTime = java.time.LocalTime.parse(startTimeStr);
                String dateStr = b.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                scheduleDetails.append(String.format("- Ngày %s | Giờ: %s | Bệnh nhân: %s | Trạng thái: %s\n",
                        dateStr, fullTimeStr, patientName, b.getStatus()));

                if (b.getStatus() != BookingStatus.CANCELED) {
                    if (b.getAppointmentDate().equals(today)) bookedToday.add(startTimeStr);
                    if (b.getAppointmentDate().equals(tomorrow)) bookedTomorrow.add(startTimeStr);

                    if (!foundNextBooked && b.getStatus() != BookingStatus.COMPLETED) {
                        java.time.LocalDateTime bookingDateTime = java.time.LocalDateTime.of(b.getAppointmentDate(), appointmentLocalTime);
                        if (bookingDateTime.isAfter(java.time.LocalDateTime.now())) {
                            nextAppointmentStr = String.format("Bệnh nhân **%s** vào ca **%s** ngày **%s**", patientName, fullTimeStr, dateStr);
                            foundNextBooked = true;
                        }
                    }
                }
            }
        }

        // 2. THUẬT TOÁN TÌM GIỜ TRỐNG GẦN NHẤT
        List<String> allWorkingSlots = List.of(
                "07:30", "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
                "13:30", "14:00", "14:30", "15:00", "15:30", "16:00", "16:30", "17:00",
                "17:30", "18:00", "18:30", "19:00"
        );

        for (String slot : allWorkingSlots) {
            java.time.LocalTime slotTime = java.time.LocalTime.parse(slot);
            if (slotTime.isAfter(nowTime) && !bookedToday.contains(slot)) {
                nextEmptySlotStr = String.format("Lúc **%s** Hôm nay (%s)", slot, today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                foundNextEmpty = true;
                break;
            }
        }
        if (!foundNextEmpty) {
            for (String slot : allWorkingSlots) {
                if (!bookedTomorrow.contains(slot)) {
                    nextEmptySlotStr = String.format("Lúc **%s** Ngày mai (%s)", slot, tomorrow.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    break;
                }
            }
        }

        // 3. Đọc dữ liệu Đánh giá
        List<Review> recentReviews = reviewRepository.findByDoctorId(doctor.getId());
        StringBuilder reviewTexts = new StringBuilder();
        int reviewCount = 0;
        for (Review r : recentReviews) {
            if (r.getComment() != null && !r.getComment().trim().isEmpty()) {
                reviewTexts.append("- ").append(r.getRating()).append(" sao: ").append(r.getComment()).append("\n");
                if (++reviewCount >= 10) break;
            }
        }
        if (reviewCount == 0) reviewTexts.append("Chưa có đánh giá chữ.\n");

        // 4. Nhồi biến vào Prompt (Đã bổ sung Tuần/Tháng và Lệnh Định Dạng)
        String contextMsg = String.format("Bạn là AI Trợ lý Bác sĩ hệ thống MediTrust. Phải trả lời bằng VĂN BẢN THƯỜNG, tự nhiên, xưng hô 'Em' với 'Bác sĩ'.\n\n"
                        + "--- THỐNG KÊ LỊCH KHÁM ---\n"
                        + "- HÔM NAY (%s): Tổng %d ca, đã khám %d ca, còn %d ca chưa khám (Tổng tất cả bao gồm hủy: %d ca)\n"
                        + "- TUẦN NÀY (%s đến %s): Tổng %d ca, đã khám %d ca, còn %d ca\n"
                        + "- THÁNG NÀY (%s): Tổng %d ca, đã khám %d ca\n"
                        + "- HỒ SƠ BỆNH ÁN CÒN NỢ: %d hồ sơ.\n"
                        + "👉 CA KHÁM GẦN NHẤT TIẾP THEO: %s\n"
                        + "👉 LỊCH TRỐNG GẦN NHẤT TIẾP THEO: %s\n\n"
                        + "--- QUY TẮC ---\n"
                        + "1. CA KHÁM VÀ LỊCH TRỐNG: Hãy lấy thông tin ở mục '👉 CA KHÁM/ LỊCH TRỐNG GẦN NHẤT TIẾP THEO' để trả lời ngay lập tức.\n"
                        + "2. HỎI VỀ THỐNG KÊ: Lấy dữ liệu ở mục 'THỐNG KÊ LỊCH KHÁM' để báo cáo chính xác cho bác sĩ (Ví dụ: Tuần này, tháng này).\n"
                        + "3. LỆNH ĐỊNH DẠNG TỐI THƯỢNG: TUYỆT ĐỐI CẤM tự chế ra các key JSON như 'total_cases'. NẾU hệ thống ngầm bắt buộc bạn phải trả về JSON (có mảng suggested_prompts), thì bạn BẮT BUỘC phải đặt TOÀN BỘ câu trả lời giao tiếp của bạn vào trong key \"ai_reply\". (Ví dụ: {\"ai_reply\": \"Dạ thưa Bác sĩ, lịch tuần này...\", \"suggested_prompts\": [...]}).\n\n"
                        + "--- REVIEW ---\n%s\n\n"
                        + "Thời gian hiện tại: %s. Người dùng hỏi: %s",
                today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), todayActiveTotal, todayCompleted, todayRemaining, todayTotal,
                startOfWeek.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), endOfWeek.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), weekTotal, weekCompleted, weekRemaining,
                startOfMonth.format(DateTimeFormatter.ofPattern("MM/yyyy")), monthTotal, monthCompleted,
                incompleteRecords,
                nextAppointmentStr,
                nextEmptySlotStr,
                reviewTexts.toString(),
                java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                request.getPrompt()
        );

        String answer = aiService.chatWithMemory(request.getSessionId() + "_doctor", contextMsg);
        return ResponseEntity.ok(Map.of("answer", answer));
    }

    @DeleteMapping("/clear/{sessionId}")
    public ResponseEntity<String> clearChat(@PathVariable String sessionId) {
        aiService.clearMemory(sessionId + "_doctor");
        return ResponseEntity.ok("Đã xóa lịch sử chat của phiên: " + sessionId);
    }
}