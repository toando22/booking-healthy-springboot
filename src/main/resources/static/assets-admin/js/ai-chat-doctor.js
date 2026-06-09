document.addEventListener('DOMContentLoaded', function() {
    // --- 1. KHỞI TẠO BIẾN ---
    const widget = document.getElementById('ai-chat-widget-doctor');
    const toggleBtn = document.getElementById('ai-chat-toggle-doctor');
    const closeBtn = document.getElementById('btn-close-doctor');
    const maximizeBtn = document.getElementById('btn-maximize-doctor');
    const chatBox = document.getElementById('ai-chat-box-doctor');
    const header = document.getElementById('ai-chat-header-doctor');
    const chatInput = document.getElementById('ai-chat-input-doctor');
    const sendBtn = document.getElementById('ai-chat-send-doctor');
    const messagesContainer = document.getElementById('ai-chat-messages-doctor');
    const btnNewChat = document.getElementById('btn-new-chat-doctor');

    // --- 2. HÀM GLOBAL (Dùng cho nút trên Header & Quick Reply) ---
    window.toggleDoctorAI = function() {
        if (chatBox.classList.contains('open')) {
            closeChat();
        } else {
            openChat();
        }
    };

    window.sendQuickReplyDoctor = function(text, btnElement) {
        if (chatInput && sendBtn) {
            chatInput.value = text;
            sendMessage();
        }
    };

    // --- 3. QUẢN LÝ PHIÊN (Session Storage) ---
    let sessionId = sessionStorage.getItem('meditrust_session_id_doctor');
    if (!sessionId) {
        sessionId = 'doc_session_' + Math.random().toString(36).substr(2, 9);
        sessionStorage.setItem('meditrust_session_id_doctor', sessionId);
    }

    const savedChatHtml = sessionStorage.getItem('meditrust_chat_html_doctor');
    if (savedChatHtml) {
        messagesContainer.innerHTML = savedChatHtml;
        setTimeout(() => {
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        }, 100);
    }

    // Kiểm tra trạng thái mở chat từ phiên trước
    const chatState = sessionStorage.getItem('meditrust_chat_state_doctor');
    if (chatState === 'open') {
        openChat();
    }

    // --- 4. HÀM MỞ / ĐÓNG CHAT CHUẨN ---
    function openChat() {
        chatBox.classList.add('open'); // Đã sửa lỗi: Dùng class 'open' thay vì 'd-none'
        //toggleBtn.classList.add('d-none');
        sessionStorage.setItem('meditrust_chat_state_doctor', 'open');
        chatInput.focus();
        loadWelcomeMessage();
        updateLiveStats();
    }

    function closeChat() {
        chatBox.classList.remove('open'); // Đã sửa lỗi
        //toggleBtn.classList.remove('d-none');
        sessionStorage.setItem('meditrust_chat_state_doctor', 'closed');

        // Reset vị trí icon nếu bị lỗi khi kéo thả
        //toggleBtn.style.cssText = "display: flex !important; visibility: visible !important; opacity: 1 !important; pointer-events: auto !important; z-index: 9999 !important; background-color: #198754 !important;";
    }

    // --- 5. LẮNG NGHE SỰ KIỆN CLICK MỞ/ĐÓNG/PHÓNG TO ---
    toggleBtn.addEventListener('click', function(e) {
        if (hasDragged) { hasDragged = false; return; } // Bỏ qua nếu đang kéo thả
        openChat();
    });

    closeBtn.addEventListener('click', (e) => {
        e.preventDefault();
        closeChat();
    });

    if (btnNewChat) {
        btnNewChat.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            if (confirm('Làm mới phiên chat?')) {
                sessionStorage.removeItem('meditrust_session_id_doctor');
                sessionStorage.removeItem('meditrust_chat_html_doctor');
                sessionStorage.setItem('meditrust_chat_state_doctor', 'open');
                window.location.reload();
            }
        });
    }

    maximizeBtn.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        const isFullscreen = chatBox.classList.toggle('fullscreen');
        if (isFullscreen) {
            maximizeBtn.innerHTML = '<i class="bi bi-fullscreen-exit"></i>';
        } else {
            maximizeBtn.innerHTML = '<i class="bi bi-arrows-fullscreen"></i>';
            chatBox.style.width  = '';
            chatBox.style.height = '';
        }
        setTimeout(() => messagesContainer.scrollTop = messagesContainer.scrollHeight, 100);
    });

    // --- 6. XỬ LÝ KÉO THẢ ICON & HEADER ---
    let isDraggingIcon = false, hasDragged = false;
    let iconOffsetX, iconOffsetY, dragStartX = 0, dragStartY = 0;

    toggleBtn.addEventListener('mousedown', function(e) {
        dragStartX = e.clientX;
        dragStartY = e.clientY;
        hasDragged = false;
        const rect = toggleBtn.getBoundingClientRect();
        iconOffsetX = e.clientX - rect.left;
        iconOffsetY = e.clientY - rect.top;
        isDraggingIcon = true;
    });

    document.addEventListener('mousemove', function(e) {
        if (!isDraggingIcon) return;
        let moveX = Math.abs(e.clientX - dragStartX);
        let moveY = Math.abs(e.clientY - dragStartY);

        if (moveX > 5 || moveY > 5) {
            hasDragged = true;
            toggleBtn.style.transition = 'none';
            toggleBtn.style.bottom = 'auto';
            toggleBtn.style.right = 'auto';

            let newX = e.clientX - iconOffsetX;
            let newY = e.clientY - iconOffsetY;

            if (newX < 0) newX = 0;
            if (newY < 0) newY = 0;
            if (newX + toggleBtn.offsetWidth > window.innerWidth) newX = window.innerWidth - toggleBtn.offsetWidth;
            if (newY + toggleBtn.offsetHeight > window.innerHeight) newY = window.innerHeight - toggleBtn.offsetHeight;

            toggleBtn.style.left = newX + 'px';
            toggleBtn.style.top = newY + 'px';
        }
    });

    document.addEventListener('mouseup', function() {
        if (isDraggingIcon) {
            isDraggingIcon = false;
            toggleBtn.style.transition = 'transform 0.2s ease';
        }
    });

    // --- 7. LOAD MESSAGE & CALL API ---
    function updateLiveStats() {
        fetch('/api/doctor/chat/welcome')
            .then(res => res.text())
            .then(newWelcomeHtml => {
                let formattedText = newWelcomeHtml.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
                const tempDiv = document.createElement('div');
                tempDiv.innerHTML = formattedText;
                const newStats = tempDiv.querySelector('#live-welcome-stats');
                const existingStatsSpan = document.getElementById('live-welcome-stats');

                if (existingStatsSpan && newStats) {
                    existingStatsSpan.innerHTML = newStats.innerHTML;
                    sessionStorage.setItem('meditrust_chat_html_doctor', messagesContainer.innerHTML);
                }
            })
            .catch(e => console.log("Lỗi cập nhật số liệu ngầm: ", e));
    }

    function loadWelcomeMessage() {
        if (messagesContainer.innerHTML.trim() === '') {
            const typingMsg = appendMessage('bot', '<div class="typing-dots"><span></span><span></span><span></span></div>');
            fetch('/api/doctor/chat/welcome')
                .then(res => res.text())
                .then(greetingText => {
                    const formattedText = greetingText.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
                    typingMsg.innerHTML = formattedText;

                    let quickActionsHtml = `
                        <div style="margin-top: 15px; padding: 12px; background: #f8f9fa; border-radius: 8px; border-left: 4px solid #198754;">
                            <div style="font-weight: bold; font-size: 13px; color: #333; margin-bottom: 8px;">
                                <i class="bi bi-lightning-charge-fill text-warning"></i> Thao tác nhanh:
                            </div>
                            <div class="quick-replies-container" style="margin-top: 0; padding-top: 0; border: none;">
                                <button class="quick-reply-btn" onclick="window.sendQuickReplyDoctor('Thống kê lịch hôm nay', this)">📊 Thống kê lịch hôm nay</button>
                                <button class="quick-reply-btn" onclick="window.sendQuickReplyDoctor('Kiểm tra bệnh án nợ', this)">📝 Kiểm tra bệnh án nợ</button>
                                <button class="quick-reply-btn" onclick="window.sendQuickReplyDoctor('Lịch tuần này thế nào?', this)">📅 Lịch tuần này</button>
                            </div>
                        </div>
                    `;
                    appendMessage('bot', quickActionsHtml);
                })
                .catch(() => {
                    typingMsg.innerHTML = 'Xin chào Bác sĩ! Em là trợ lý AI thống kê lịch khám.';
                });
        }
    }

    function appendMessage(sender, textOrHtml) {
        const msgDiv = document.createElement('div');
        msgDiv.classList.add('chat-msg', sender === 'assistant' || sender === 'bot' ? 'bot' : 'user');

        // Tích hợp marked.js để parse markdown từ file số 2
        if((sender === 'bot' || sender === 'assistant') && textOrHtml.indexOf('<div') === -1 && typeof marked !== 'undefined') {
            msgDiv.innerHTML = marked.parse(textOrHtml);
        } else {
            msgDiv.innerHTML = textOrHtml;
        }

        messagesContainer.appendChild(msgDiv);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
        sessionStorage.setItem('meditrust_chat_html_doctor', messagesContainer.innerHTML);
        return msgDiv;
    }

    async function sendMessage() {
        const text = chatInput.value.trim();
        if (!text) return;

        appendMessage('user', text);
        chatInput.value = '';

        const typingMsg = appendMessage('bot', '<div class="typing-dots"><span></span><span></span><span></span></div>');

        try {
            // Sử dụng API endpoint duy nhất (bạn có thể đổi thành /api/doctor/assistant/chat nếu backend yêu cầu)
            const response = await fetch('/api/doctor/chat/ask', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sessionId: sessionId, prompt: text })
            });

            if (response.ok) {
                const data = await response.json();
                let cleanStr = data.answer || data.ai_reply || "";

                // Cố gắng parse JSON nếu backend trả về string JSON bọc trong markdown
                cleanStr = cleanStr.replace(/```json/gi, '').replace(/```/g, '').trim();
                let formattedText = cleanStr;

                try {
                    const aiData = JSON.parse(cleanStr);
                    if (aiData.ai_reply) {
                        formattedText = aiData.ai_reply;
                    }
                } catch(e) {
                    // Fallback to raw string
                }

                if(typeof marked !== 'undefined') {
                    typingMsg.innerHTML = marked.parse(formattedText);
                } else {
                    typingMsg.innerHTML = formattedText.replace(/\n/g, '<br>').replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
                }

                sessionStorage.setItem('meditrust_chat_html_doctor', messagesContainer.innerHTML);
            } else {
                typingMsg.innerHTML = 'Hệ thống bận, vui lòng thử lại sau.';
            }
        } catch (error) {
            typingMsg.innerHTML = 'Lỗi kết nối. Vui lòng kiểm tra mạng.';
            console.error("AI Chat Error:", error);
        }
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    sendBtn.addEventListener('click', sendMessage);
    chatInput.addEventListener('keypress', (e) => { if (e.key === 'Enter') sendMessage(); });

    // --- 8. TOUR GUIDE CẢNH BÁO Y TẾ CHO BÁC SĨ ---
    function checkDoctorEmergencyAlert() {
        if (sessionStorage.getItem('meditrust_chat_state_doctor') === 'open') return;

        fetch('/api/public/news/latest-alert')
            .then(response => {
                if (response.status === 204) return null;
                return response.json();
            })
            .then(data => {
                if (data) {
                    const lastAlertId = localStorage.getItem('meditrust_doctor_last_alert_id');
                    if (lastAlertId === data.id.toString()) return;

                    const style = document.createElement('style');
                    style.innerHTML = `
                        .tour-guide-box-doctor { position: fixed; bottom: 110px; right: 25px; width: 320px; background: #fff; border: 2px solid #198754; border-radius: 12px; padding: 16px; box-shadow: 0 10px 30px rgba(25, 135, 84, 0.25); z-index: 10000; opacity: 0; visibility: hidden; transform: translateY(20px) scale(0.9); transition: all 0.5s cubic-bezier(0.68, -0.55, 0.265, 1.55); }
                        .tour-guide-box-doctor.show { opacity: 1; visibility: visible; transform: translateY(0) scale(1); }
                        .tour-guide-box-doctor::after { content: ''; position: absolute; bottom: -12px; right: 22px; border-width: 12px 12px 0; border-style: solid; border-color: #198754 transparent transparent transparent; }
                        .tour-guide-title-doc { font-weight: 800; color: #dc3545; margin-bottom: 8px; font-size: 15px; display: flex; align-items: center; gap: 8px; }
                        .tour-guide-desc-doc { font-size: 13px; color: #444; margin-bottom: 12px; line-height: 1.5; }
                        .tour-guide-btn-doc { background: #198754; color: white; border: none; padding: 6px 16px; border-radius: 20px; font-size: 12px; cursor: pointer; font-weight: bold; transition: 0.2s; }
                        .tour-guide-btn-doc:hover { transform: scale(1.05); }
                    `;
                    document.head.appendChild(style);

                    const tourGuideHtml = `
                        <div id="chat-tour-guide-doctor" class="tour-guide-box-doctor show">
                            <div class="tour-guide-title-doc"><i class="bi bi-exclamation-triangle-fill fs-4"></i> Cập nhật Dịch tễ Khẩn cấp</div>
                            <div class="tour-guide-desc-doc">
                                <strong>${data.title}</strong><br/>
                                <span style="font-size: 0.9em;">${data.summary}</span>
                                <div style="margin-top: 8px; padding-top: 8px; border-top: 1px dashed #ccc; font-style: italic; color: #198754;">
                                    Bác sĩ vui lòng lưu ý các triệu chứng lâm sàng liên quan trong quá trình khám bệnh.
                                </div>
                            </div>
                            <div style="display: flex; gap: 10px; margin-top: 10px; justify-content: flex-end;">
                                <button id="btn-skip-alert-doc" class="tour-guide-btn-doc" style="background-color: #6c757d;">Đã nắm rõ</button>
                                <button id="btn-read-alert-doc" class="tour-guide-btn-doc">Xem chi tiết</button>
                            </div>
                        </div>
                    `;
                    document.body.insertAdjacentHTML('beforeend', tourGuideHtml);

                    const tourGuideBox = document.getElementById('chat-tour-guide-doctor');

                    document.getElementById('btn-read-alert-doc').addEventListener('click', function() {
                        localStorage.setItem('meditrust_doctor_last_alert_id', data.id.toString());
                        window.open('/news/' + data.id, '_blank');
                        tourGuideBox.classList.remove('show');
                    });

                    document.getElementById('btn-skip-alert-doc').addEventListener('click', function() {
                        localStorage.setItem('meditrust_doctor_last_alert_id', data.id.toString());
                        tourGuideBox.classList.remove('show');
                    });

                    toggleBtn.addEventListener('click', function() {
                        if (tourGuideBox) tourGuideBox.classList.remove('show');
                    });
                }
            })
            .catch(err => console.error("Lỗi khi tải tin tức khẩn cấp (Doctor):", err));
    }

    setTimeout(checkDoctorEmergencyAlert, 2000);
});