package com.bookinghealthy.service;

import com.bookinghealthy.dto.ai.AiMessage;
import com.bookinghealthy.dto.ai.AiRequest;
import com.bookinghealthy.dto.ai.AiResponse;
import com.bookinghealthy.model.AiChatSession;
import com.bookinghealthy.model.Department; // Bổ sung Entity Khoa
import com.bookinghealthy.repository.AiChatSessionRepository;
import com.bookinghealthy.repository.DepartmentRepository; // Bổ sung Repository Khoa
import com.bookinghealthy.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiService {

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.api.key}")
    private String apiKey;

    @Autowired private RestTemplate restTemplate;
    @Autowired private AiChatSessionRepository sessionRepository;
    @Autowired private UserRepository userRepository;

    // THAY THẾ AiRuleRepository BẰNG DepartmentRepository
    @Autowired private DepartmentRepository departmentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================================================================
    // 1. BASE PROMPT MỚI: KIẾN TRÚC AI AGENT (ĐỊNH TUYẾN NGỮ NGHĨA)
    // =========================================================================
    private static final String BASE_PROMPT =
            "Bạn là Chuyên gia Phân luồng Bệnh nhân (Triage AI Agent) của hệ thống y tế MediTrust.\n" +
                    "MỤC TIÊU CỦA BẠN: Lắng nghe triệu chứng, an ủi bệnh nhân và ĐIỀU HƯỚNG họ đến đúng Chuyên khoa phù hợp nhất.\n\n" +
                    "BẮT BUỘC TỐI THƯỢNG: Trả lời của bạn phải là MỘT CHUỖI JSON HỢP LỆ. KHÔNG ĐƯỢC CHỨA VĂN BẢN NÀO NGOÀI JSON.\n\n" +
                    "=== 1. KIẾN THỨC MẶC ĐỊNH VỀ PHÒNG KHÁM ===\n" +
                    "- Địa chỉ: 123 Đường Y Tế, Quận Trung Tâm, TP. Hà Nội.\n" +
                    "- Giờ làm việc: 07:30 - 20:30 TẤT CẢ các ngày trong tuần (Kể cả Thứ 7 và Chủ Nhật).\n" +
                    "- Chi phí & Bảo hiểm: Minh bạch trên website, có áp dụng BHYT.\n" +
                    "- Đặt lịch: Khuyên khách hàng chọn bác sĩ trên web hoặc mô tả bệnh để bạn điều hướng.\n\n" +

                    "=== 2. QUY TẮC PHÂN LUỒNG & BẢO ĐẢM AN TOÀN Y KHOA ===\n" +
                    "- NẾU LÀ CÂU HỎI THÔNG THƯỜNG (địa chỉ, giờ làm, giá cả, cách đặt lịch, chào hỏi): Trả lời thân thiện bằng KIẾN THỨC MẶC ĐỊNH. TUYỆT ĐỐI KHÔNG chèn mã chuyên khoa.\n" +
                    "- NẾU BỆNH NHÂN KỂ TRIỆU CHỨNG BỆNH LÝ, HÃY ÁP DỤNG TƯ DUY PHÂN LUỒNG SAU ĐÂY:\n" +
                    "  + TRƯỜNG HỢP 1 (TRIỆU CHỨNG RÕ RÀNG): Bệnh nhân kể triệu chứng đặc thù (VD: đau răng, mỏi gáy). Hãy thể hiện sự đồng cảm -> Tư vấn mẹo sơ cứu tại nhà -> Khuyên đặt lịch -> BẮT BUỘC chèn mã [BOOK_DEPT_{ID}] vào cuối câu (Thay {ID} bằng ID Khoa tương ứng).\n" +
                    "  + TRƯỜNG HỢP 2 (TRIỆU CHỨNG ĐA KHOA): Triệu chứng khớp với từ 2 khoa trở lên (VD: tức ngực, khó thở có thể là Tim mạch hoặc Hô hấp). KÍCH HOẠT CHẾ ĐỘ HỎI DÒ (Clarification Mode): TUYỆT ĐỐI KHÔNG chèn mã khoa [BOOK_DEPT] ở lượt này. Hãy đặt 1-2 câu hỏi để phân biệt (VD: 'Bạn có ho có đờm không, hay đau nhói lan ra tay?').\n" +
                    "  + TRƯỜNG HỢP 3 (CƠ CHẾ FALLBACK - AN TOÀN LÀ TRÊN HẾT): Nếu bệnh nhân mô tả mông lung, phức tạp, hoặc sau khi 'Hỏi dò' vẫn không thể xác định khoa chính xác. Hãy khuyên họ đặt lịch tại 'Khoa Y học gia đình' để bác sĩ khám tổng quát và sàng lọc ban đầu. BẮT BUỘC nói câu: 'Hệ thống hiện đang ghi nhận bác sĩ Tổng quát có lịch trống. Lịch này có thể hết rất nhanh, bạn vui lòng click vào lịch bên dưới để giữ chỗ ngay nhé.' -> BẮT BUỘC chèn mã [BOOK_DEPT_22].\n" +
                    "- NẾU TRIỆU CHỨNG NGUY HIỂM (đau tim, đột quỵ, nôn máu...): Bỏ qua hỏi thăm, yêu cầu đi cấp cứu ngay lập tức, và chèn mã Khoa Cấp cứu [BOOK_DEPT_21].\n" +
                    "- LỆNH CẤM: Từ chối lịch sự các chủ đề ngoài y tế.\n\n" +

                    "=== 3. CẢNH BÁO Y KHOA ===\n" +
                    "- Nếu trả lời về bệnh lý, luôn kết thúc bằng: '⚠️ Lưu ý: Đây chỉ là tư vấn sơ bộ từ AI, bạn nên đến cơ sở y tế để được chẩn đoán chính xác.'\n" +
                    "- Nếu chỉ trả lời địa chỉ/giờ làm: KHÔNG cần chèn câu cảnh báo này.\n\n" +

                    "=== 4. DANH SÁCH CHUYÊN KHOA HIỆN CÓ CỦA MEDITRUST ===\n";

    @Transactional
    public String chatWithMemory(String sessionId, String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set("HTTP-Referer", "http://localhost:8080");

        // =========================================================================
        // 2. DYNAMIC CONTEXT: NẠP DANH SÁCH KHOA TỪ DATABASE VÀO PROMPT
        // =========================================================================
        StringBuilder deptsInfo = new StringBuilder();
        try {
            List<Department> depts = departmentRepository.findAll();
            if (depts.isEmpty()) {
                deptsInfo.append("- (Hệ thống đang cập nhật danh sách chuyên khoa. Hãy khuyên bệnh nhân khám Tổng Quát).\n");
            } else {
                for (Department d : depts) {
                    // Lấy ID, Tên Khoa và Mô tả. Tránh lỗi NullPointerException nếu description trống
                    String desc = (d.getDescription() != null && !d.getDescription().isEmpty())
                            ? d.getDescription()
                            : "Khám và điều trị các bệnh lý liên quan đến khoa này.";
                    deptsInfo.append("- ID: ").append(d.getId())
                            .append(" | Tên Khoa: ").append(d.getName())
                            .append(" | Chuyên trị: ").append(desc).append("\n");
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách Khoa: " + e.getMessage());
        }

        // Ép toàn bộ vào System Prompt cuối cùng
        String finalSystemPrompt = BASE_PROMPT + deptsInfo.toString();

        // =========================================================================
        // 3. XỬ LÝ SESSION & USER LOGIC (GIỮ NGUYÊN 100%)
        // =========================================================================
        AiChatSession chatSession = sessionRepository.findBySessionCode(sessionId).orElseGet(() -> {
            AiChatSession newSession = new AiChatSession();
            newSession.setSessionCode(sessionId);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                Object principal = auth.getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                    userRepository.findByUsername(username).ifPresent(newSession::setUser);
                }
                else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                    String email = ((org.springframework.security.oauth2.core.user.OAuth2User) principal).getAttribute("email");
                    if (email != null) {
                        userRepository.findByEmail(email).ifPresent(newSession::setUser);
                    }
                }
                else {
                    String name = auth.getName();
                    userRepository.findByUsername(name).ifPresentOrElse(
                            newSession::setUser,
                            () -> userRepository.findByEmail(name).ifPresent(newSession::setUser)
                    );
                }
            }
            return sessionRepository.save(newSession);
        });

        try {
            List<AiMessage> chatHistory = new ArrayList<>();
            if (chatSession.getChatHistoryJson() != null && !chatSession.getChatHistoryJson().equals("[]")) {
                chatHistory = objectMapper.readValue(chatSession.getChatHistoryJson(), new TypeReference<List<AiMessage>>(){});
            }

            chatHistory.add(new AiMessage("user", userPrompt));

            List<AiMessage> messagesToSend = new ArrayList<>();
            messagesToSend.add(new AiMessage("system", finalSystemPrompt));
            int startIndex = Math.max(0, chatHistory.size() - 10);
            messagesToSend.addAll(chatHistory.subList(startIndex, chatHistory.size()));

            // =========================================================================
            // 4. GỌI API (LLM SẼ TỰ ĐỘNG XUẤT RA MÃ TAG)
            // =========================================================================
            String[] fallbackModels = { "openai/gpt-4o-mini", "openrouter/free", "google/gemini-2.0-flash-exp:free" };
            for (String modelName : fallbackModels) {
                try {
                    AiRequest request = new AiRequest();
                    request.setModel(modelName);
                    request.setMessages(messagesToSend);
                    request.setTemperature(0.2); // Giữ nhiệt độ thấp để AI chọn mã ID chính xác, không bịa

                    AiResponse response = restTemplate.postForObject(apiUrl, new HttpEntity<>(request, headers), AiResponse.class);

                    if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                        String aiAnswer = response.getChoices().get(0).getMessage().getContent();

                        // Lưu vào Database
                        chatHistory.add(new AiMessage("assistant", aiAnswer));
                        chatSession.setChatHistoryJson(objectMapper.writeValueAsString(chatHistory));
                        sessionRepository.save(chatSession);

                        return aiAnswer;
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Lỗi model: " + modelName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Hệ thống bận. Thử lại sau.";
    }

    // === HỆ THỐNG DỌN RÁC TỰ ĐỘNG (GIỮ NGUYÊN) ===
    @Transactional
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldGuestSessions() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        sessionRepository.deleteGuestSessionsOlderThan(cutoffDate);
        System.out.println("🧹 Đã dọn dẹp xong lịch sử chat rác của khách vãng lai.");
    }

    // === HÀM XÓA LỊCH SỬ CHAT (GIỮ NGUYÊN) ===
    @Transactional
    public void clearMemory(String sessionId) {
        sessionRepository.findBySessionCode(sessionId).ifPresent(session -> {
            sessionRepository.delete(session);
        });
    }
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
    // Dùng ConcurrentHashMap để đảm bảo Thread-Safe (An toàn khi nhiều luồng truy cập)
    private final java.util.concurrent.ConcurrentHashMap<String, SlotLock> softLockCache = new java.util.concurrent.ConcurrentHashMap<>();

    // Job tự động dọn dẹp các Lock đã hết hạn (Chạy mỗi 1 phút)
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000)
    public void cleanUpExpiredLocks() {
        long now = System.currentTimeMillis();
        softLockCache.entrySet().removeIf(entry -> now > entry.getValue().expireAtMillis);
    }
}