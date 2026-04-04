package com.bookinghealthy.controller.api;

import com.bookinghealthy.dto.DoctorDTO;
import com.bookinghealthy.dto.ai.ChatRequest;
import com.bookinghealthy.model.Doctor;
import com.bookinghealthy.model.Schedule;
import com.bookinghealthy.repository.ScheduleRepository;
import com.bookinghealthy.service.AiService;
import com.bookinghealthy.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class AiController {

    @Autowired
    private AiService aiService;

    // === INJECT THÊM DOCTOR SERVICE ĐỂ LẤY DỮ LIỆU ===
    @Autowired
    private DoctorService doctorService;

    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private com.bookinghealthy.repository.AiChatSessionRepository sessionRepository;
    @Autowired private com.bookinghealthy.repository.UserRepository userRepository;
    @Autowired private com.bookinghealthy.repository.BookingRepository bookingRepository; // INJECT THÊM REPO NÀY
    // THÊM REPOSITORY NÀY LÊN ĐẦU FILE CÙNG CÁC @Autowired KHÁC
    @Autowired private com.bookinghealthy.repository.DoctorBlockTimeRepository doctorBlockTimeRepository;
    // INJECT SERVICE THẦN THÁNH CỦA BẠN VÀO ĐÂY
    @Autowired private com.bookinghealthy.service.TimeSlotService timeSlotService;


    // =========================================================================
    // CƠ CHẾ SOFT-LOCK (MÔ PHỎNG REDIS TTL) - XỬ LÝ RACE CONDITION
    // =========================================================================
    static class SlotLock {
        String sessionId;
        long expireAtMillis;
        public SlotLock(String sessionId, long expireAtMillis) {
            this.sessionId = sessionId;
            this.expireAtMillis = expireAtMillis;
        }
    }

    // Bộ nhớ đệm lưu trữ các khóa (Khóa tự động mất sau 3 phút)
    private final java.util.concurrent.ConcurrentHashMap<String, SlotLock> softLockCache = new java.util.concurrent.ConcurrentHashMap<>();

    // Job tự động dọn dẹp các Lock đã hết hạn (Chạy mỗi 1 phút)
    @Scheduled(fixedRate = 60000)
    public void cleanUpExpiredLocks() {
        long now = System.currentTimeMillis();
        softLockCache.entrySet().removeIf(entry -> now > entry.getValue().expireAtMillis);
    }

    // --------------------------------------------------------
    // 1. CÁC API DÀNH CHO XỬ LÝ NGÔN NGỮ (LLM)
    // --------------------------------------------------------

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askAi(@RequestBody ChatRequest request) {
        String answer = aiService.chatWithMemory(request.getSessionId(), request.getPrompt());
        Map<String, String> result = new HashMap<>();
        result.put("answer", answer);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/clear/{sessionId}")
    public ResponseEntity<String> clearChat(@PathVariable String sessionId) {
        aiService.clearMemory(sessionId);
        return ResponseEntity.ok("Đã xóa lịch sử chat của phiên: " + sessionId);
    }

    // BỘ KHUNG GIỜ CHUẨN (Copy y hệt từ BookingApi của mày)
    private final String[] ALL_SLOTS = {
            "07:30 - 08:00", "08:00 - 08:30", "08:30 - 09:00", "09:00 - 09:30",
            "09:30 - 10:00", "10:00 - 10:30", "10:30 - 11:00", "11:00 - 11:30",
            "13:30 - 14:00", "14:00 - 14:30", "14:30 - 15:00", "15:00 - 15:30",
            "15:30 - 16:00", "16:00 - 16:30", "16:30 - 17:00", "17:00 - 17:30",
            "17:30 - 18:00", "18:00 - 18:30", "18:30 - 19:00", "19:00 - 19:30",
            "19:30 - 20:00", "20:00 - 20:30"
    };
    // =========================================================================
    // API LẤY DATA BÁC SĨ (COPY 100% LOGIC TỪ BOOKING API, BỎ QUA BẢNG SCHEDULE)
    // =========================================================================
    @GetMapping("/doctors/department/{departmentId}")
    public ResponseEntity<List<DoctorDTO>> getDoctorsByDepartment(@PathVariable Long departmentId, @RequestParam(required = false) String sessionId) {

        List<Doctor> doctors = doctorService.findByDepartmentId(departmentId);
        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM");
        java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");

        long nowMillis = System.currentTimeMillis();

        List<DoctorDTO> doctorDtos = doctors.stream()
                .limit(3)
                .map(doc -> {
                    DoctorDTO dto = new DoctorDTO(doc);
                    List<String> availableSlots = new java.util.ArrayList<>();

                    java.time.LocalDate today = java.time.LocalDate.now();
                    java.time.LocalTime now = java.time.LocalTime.now();
                    java.time.LocalDate endDate = today.plusDays(7); // Quét 7 ngày tới

                    // Quét từng ngày, bắt đầu từ HÔM NAY
                    for (java.time.LocalDate date = today; date.isBefore(endDate); date = date.plusDays(1)) {

                        // 1. Kéo dữ liệu các giờ ĐÃ BỊ ĐẶT (Booking)
                        List<com.bookinghealthy.model.Booking> bookings = bookingRepository
                                .findByDoctorIdAndAppointmentDateAndStatusNot(doc.getId(), date, com.bookinghealthy.model.BookingStatus.CANCELED);
                        List<String> bookedTimes = bookings.stream()
                                .map(com.bookinghealthy.model.Booking::getAppointmentTime)
                                .collect(Collectors.toList());

                        // 2. Kéo dữ liệu các giờ BỊ CHẶN (DoctorBlockTime)
                        List<com.bookinghealthy.model.DoctorBlockTime> blockedTimes = doctorBlockTimeRepository
                                .findByDoctorIdAndBlockDate(doc.getId(), date);

                        // 3. Duyệt mảng ALL_SLOTS để tìm giờ TRỐNG
                        for (String slotStr : ALL_SLOTS) {
                            String[] parts = slotStr.split(" - ");
                            java.time.LocalTime slotStart = java.time.LocalTime.parse(parts[0], timeFormatter);
                            java.time.LocalTime slotEnd = java.time.LocalTime.parse(parts[1], timeFormatter);

                            // Lọc 1: Bỏ qua giờ trong quá khứ (nếu là ngày hôm nay)
                            if (date.isEqual(today) && slotStart.isBefore(now)) {
                                continue;
                            }

                            // Lọc 2: Bỏ qua giờ đã có khách đặt
                            if (bookedTimes.contains(slotStr)) {
                                continue;
                            }

                            // Lọc 3: Bỏ qua giờ Bác sĩ tự chặn (Overlap logic y hệt BookingApi)
                            boolean isBlocked = false;
                            for (com.bookinghealthy.model.DoctorBlockTime block : blockedTimes) {
                                if (slotStart.isBefore(block.getEndTime()) && slotEnd.isAfter(block.getStartTime())) {
                                    isBlocked = true;
                                    break;
                                }
                            }
                            if (isBlocked) continue;

                            // === BẮT ĐẦU CHÈN LỌC 4: RACE CONDITION SOFT-LOCK CHECK ===
                            String lockKey = doc.getId() + "_" + date.toString() + "_" + slotStr;
                            SlotLock existingLock = softLockCache.get(lockKey);

                            if (existingLock != null) {
                                if (nowMillis > existingLock.expireAtMillis) {
                                    // Lock đã hết hạn -> Xóa rác
                                    softLockCache.remove(lockKey);
                                } else if (sessionId != null && !sessionId.equals(existingLock.sessionId)) {
                                    // Lock còn hạn VÀ đang bị thằng khác giành -> BỎ QUA SLOT NÀY
                                    continue;
                                }
                            }

                            // ĐỦ ĐIỀU KIỆN TRỐNG -> KHÓA LẠI CHO USER NÀY TRONG 3 PHÚT (180,000 ms)
                            if (sessionId != null) {
                                softLockCache.put(lockKey, new SlotLock(sessionId, nowMillis + 180000));
                            }
                            // === KẾT THÚC CHÈN LỌC 4 ===

                            // NẾU VƯỢ QUA 3 BỘ LỌC TRÊN -> CHÍNH LÀ GIỜ TRỐNG!
                            String displaySlot = translateDay(date.getDayOfWeek()) + " " + date.format(dateFormatter) + " (" + slotStr + ")";
                            availableSlots.add(displaySlot);

                            if (availableSlots.size() >= 4) break; // Lấy 4 slot thôi cho UI gọn
                        }

                        // QUAN TRỌNG: Đã tìm thấy giờ trống của ngày gần nhất thì DỪNG LUÔN, không nhảy ngày hôm sau nữa!
                        if (!availableSlots.isEmpty()) {
                            break;
                        }
                    }

                    dto.setAvailableSlots(availableSlots);
                    return dto;
                })
                .collect(Collectors.toList());
        System.out.println(">>> Đang lấy lịch cho Session: " + sessionId);
        return ResponseEntity.ok(doctorDtos);
    }
    // API: Kéo lịch sử chat của User đang đăng nhập
    @GetMapping("/history")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getMyHistory() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }

        // --- TÌM CHÍNH XÁC USER ĐANG ĐĂNG NHẬP ---
        java.util.Optional<com.bookinghealthy.model.User> currentUserOpt = java.util.Optional.empty();
        Object principal = auth.getPrincipal();

        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            currentUserOpt = userRepository.findByUsername(username);
        } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
            String email = ((org.springframework.security.oauth2.core.user.OAuth2User) principal).getAttribute("email");
            if (email != null) currentUserOpt = userRepository.findByEmail(email);
        } else {
            String name = auth.getName();
            currentUserOpt = userRepository.findByUsername(name);
            if (currentUserOpt.isEmpty()) currentUserOpt = userRepository.findByEmail(name);
        }

        // --- TRẢ VỀ LỊCH SỬ NẾU TÌM THẤY USER ---
        return currentUserOpt.map(user -> {
            java.util.List<com.bookinghealthy.model.AiChatSession> sessions = sessionRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
            java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();

            for (com.bookinghealthy.model.AiChatSession s : sessions) {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("sessionCode", s.getSessionCode());
                map.put("date", s.getUpdatedAt().toString());
                map.put("chatData", s.getChatHistoryJson());
                result.add(map);
            }
            return ResponseEntity.ok(result);
        }).orElse(ResponseEntity.ok(java.util.Collections.emptyList()));
    }

    // Hàm phụ trợ dịch Ngày sang tiếng Việt
    private String translateDay(DayOfWeek day) {
        switch(day) {
            case MONDAY: return "T2";
            case TUESDAY: return "T3";
            case WEDNESDAY: return "T4";
            case THURSDAY: return "T5";
            case FRIDAY: return "T6";
            case SATURDAY: return "T7";
            case SUNDAY: return "CN";
            default: return "";
        }
    }
}