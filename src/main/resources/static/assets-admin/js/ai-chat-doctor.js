document.addEventListener('DOMContentLoaded', function() {
    const widget = document.getElementById('ai-chat-widget-doctor');
    const toggleBtn = document.getElementById('ai-chat-toggle-doctor');
    const closeBtn = document.getElementById('btn-close-doctor');
    const maximizeBtn = document.getElementById('btn-maximize-doctor');
    const chatBox = document.getElementById('ai-chat-box-doctor');
    const header = document.getElementById('ai-chat-header-doctor');
    const chatInput = document.getElementById('ai-chat-input-doctor');
    const sendBtn = document.getElementById('ai-chat-send-doctor');
    const messagesContainer = document.getElementById('ai-chat-messages-doctor');

    window.sendQuickReplyDoctor = function(text, btnElement) {
        if (chatInput && sendBtn) {
            chatInput.value = text;
            sendBtn.click();
        }
    };

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

    const chatState = sessionStorage.getItem('meditrust_chat_state_doctor');
    if (chatState === 'open') {
        chatBox.classList.remove('d-none');
        toggleBtn.classList.add('d-none');
        loadWelcomeMessage();
    }

    let isDraggingIcon = false, hasDragged = false;
    let iconOffsetX, iconOffsetY, dragStartX = 0, dragStartY = 0;

    toggleBtn.addEventListener('mousedown', function(e) {
        dragStartX = e.clientX;
        dragStartY = e.clientY;
        hasDragged = false;
        const rect = widget.getBoundingClientRect();
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
            widget.style.transition = 'none';
            widget.style.bottom = 'auto';
            widget.style.right = 'auto';
            let newX = e.clientX - iconOffsetX;
            let newY = e.clientY - iconOffsetY;
            if (newX < 0) newX = 0;
            if (newY < 0) newY = 0;
            if (newX + widget.offsetWidth > window.innerWidth) newX = window.innerWidth - widget.offsetWidth;
            if (newY + toggleBtn.offsetHeight > window.innerHeight) newY = window.innerHeight - toggleBtn.offsetHeight;
            widget.style.left = newX + 'px';
            widget.style.top = newY + 'px';
        }
    });

    document.addEventListener('mouseup', function() {
        if (isDraggingIcon) {
            isDraggingIcon = false;
            widget.style.transition = 'all 0.3s ease';
        }
    });

    toggleBtn.addEventListener('click', function(e) {
        if (hasDragged) { hasDragged = false; return; }
        chatBox.classList.remove('d-none');
        toggleBtn.classList.add('d-none');
        sessionStorage.setItem('meditrust_chat_state_doctor', 'open');
        chatInput.focus();
        loadWelcomeMessage();

        // --- THÊM ĐOẠN NÀY ĐỂ LIVE UPDATE SỐ LIỆU MÀ KHÔNG CẦN CLEAR LỊCH SỬ CHAT ---
        fetch('/api/doctor/chat/welcome')
            .then(res => res.text())
            .then(newWelcomeHtml => {
                // Định dạng lại các thẻ in đậm
                let formattedText = newWelcomeHtml.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

                // Trích xuất nội dung bên trong thẻ <span id='live-welcome-stats'> của backend trả về
                const tempDiv = document.createElement('div');
                tempDiv.innerHTML = formattedText;
                const newStats = tempDiv.querySelector('#live-welcome-stats');

                // Ghi đè vào dòng tin nhắn chào mừng đầu tiên đang có sẵn trên màn hình
                const existingStatsSpan = document.getElementById('live-welcome-stats');
                if (existingStatsSpan && newStats) {
                    existingStatsSpan.innerHTML = newStats.innerHTML;
                    sessionStorage.setItem('meditrust_chat_html_doctor', messagesContainer.innerHTML); // Lưu lại
                }
            })
            .catch(e => console.log("Lỗi cập nhật số liệu ngầm: ", e));
        // -------------------------------------------------------------------------
    });

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

    closeBtn.addEventListener('click', (e) => {
        e.preventDefault();
        chatBox.classList.add('d-none');
        toggleBtn.classList.remove('d-none');
        sessionStorage.setItem('meditrust_chat_state_doctor', 'closed');
        toggleBtn.style.cssText = "display: flex !important; visibility: visible !important; opacity: 1 !important; pointer-events: auto !important; z-index: 9999 !important; background-color: #198754 !important;";
        widget.style.transition = 'none';
        widget.style.top = 'auto';
        widget.style.left = 'auto';
        widget.style.bottom = '20px';
        widget.style.right = '20px';
        hasDragged = false;
        isDraggingIcon = false;
    });

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

    // Kéo thả bằng header
    let isDraggingHeader = false, startHeaderX, startHeaderY, startBoxX, startBoxY;
    header.addEventListener('mousedown', (e) => {
        if(chatBox.classList.contains('fullscreen')) return;
        isDraggingHeader = true;
        startHeaderX = e.clientX;
        startHeaderY = e.clientY;
        const rect = widget.getBoundingClientRect();
        widget.style.bottom = 'auto';
        widget.style.right = 'auto';
        startBoxX = rect.left;
        startBoxY = rect.top;
    });

    document.addEventListener('mousemove', (e) => {
        if (!isDraggingHeader) return;
        let newX = startBoxX + (e.clientX - startHeaderX);
        let newY = startBoxY + (e.clientY - startHeaderY);
        widget.style.left = newX + 'px';
        widget.style.top = newY + 'px';
    });
    document.addEventListener('mouseup', () => isDraggingHeader = false);

    function appendMessage(sender, htmlContent) {
       const msgDiv = document.createElement('div');
       msgDiv.classList.add('chat-msg', sender === 'assistant' || sender === 'bot' ? 'bot' : 'user');
       msgDiv.innerHTML = htmlContent;
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
            const response = await fetch('/api/doctor/chat/ask', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sessionId: sessionId, prompt: text })
            });

            if (response.ok) {
                const data = await response.json();
                let cleanStr = data.answer.replace(/```json/gi, '').replace(/```/g, '').trim();
                let formattedText = cleanStr;

                try {
                    const aiData = JSON.parse(cleanStr);
                    if (aiData.ai_reply) {
                        formattedText = aiData.ai_reply;
                    }
                } catch(e) {
                    // fall back
                }

                typingMsg.innerHTML = formattedText.replace(/\n/g, '<br>').replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
                sessionStorage.setItem('meditrust_chat_html_doctor', messagesContainer.innerHTML);
            } else {
                typingMsg.innerHTML = 'Hệ thống bận.';
            }
        } catch (error) {
            typingMsg.innerHTML = 'Lỗi kết nối.';
        }
    }

    sendBtn.addEventListener('click', sendMessage);
    chatInput.addEventListener('keypress', (e) => { if (e.key === 'Enter') sendMessage(); });

    const btnNewChat = document.getElementById('btn-new-chat-doctor');
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

    // ==========================================
    // TOUR GUIDE CẢNH BÁO Y TẾ CHO BÁC SĨ
    // ==========================================
    function checkDoctorEmergencyAlert() {
        // Nếu khung chat đang mở thì không hiện Tour Guide
        if (sessionStorage.getItem('meditrust_chat_state_doctor') === 'open') return;

        fetch('/api/public/news/latest-alert')
            .then(response => {
                if (response.status === 204) {
                    return null;
                }
                return response.json();
            })
            .then(data => {
                if (data) {
                    const lastAlertId = localStorage.getItem('meditrust_doctor_last_alert_id');
                    if (lastAlertId === data.id.toString()) {
                        return; // Đã xem rồi
                    }

                    // CSS cho Tour Guide Bác sĩ
                    const style = document.createElement('style');
                    style.innerHTML = `
                        .tour-guide-box-doctor {
                            position: fixed;
                            bottom: 110px;
                            right: 25px;
                            width: 320px;
                            background: #fff;
                            border: 2px solid #198754;
                            border-radius: 12px;
                            padding: 16px;
                            box-shadow: 0 10px 30px rgba(25, 135, 84, 0.25);
                            z-index: 10000;
                            opacity: 0;
                            visibility: hidden;
                            transform: translateY(20px) scale(0.9);
                            transition: all 0.5s cubic-bezier(0.68, -0.55, 0.265, 1.55);
                        }
                        .tour-guide-box-doctor.show {
                            opacity: 1;
                            visibility: visible;
                            transform: translateY(0) scale(1);
                        }
                        .tour-guide-box-doctor::after {
                            content: '';
                            position: absolute;
                            bottom: -12px;
                            right: 22px;
                            border-width: 12px 12px 0;
                            border-style: solid;
                            border-color: #198754 transparent transparent transparent;
                        }
                        .tour-guide-title-doc { font-weight: 800; color: #dc3545; margin-bottom: 8px; font-size: 15px; display: flex; align-items: center; gap: 8px; }
                        .tour-guide-desc-doc { font-size: 13px; color: #444; margin-bottom: 12px; line-height: 1.5; }
                        .tour-guide-btn-doc {
                            background: #198754; color: white; border: none; padding: 6px 16px; border-radius: 20px; font-size: 12px; cursor: pointer; font-weight: bold; transition: 0.2s;
                        }
                        .tour-guide-btn-doc:hover { transform: scale(1.05); }
                    `;
                    document.head.appendChild(style);

                    // HTML cho Tour Guide Bác sĩ
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
                        window.open('/admin/manage-news/edit/' + data.id, '_blank'); // Mở bài viết trong trang quản lý hoặc trang nào đó phù hợp. Tạm dùng link edit (nếu Bác sĩ có quyền) hoặc trang xem chung. Hoặc đơn giản là ẩn và để bác sĩ vào mục tin tức. Thực tế Bác sĩ có thể không vào đc admin/manage-news. Tạm trỏ về màn Dashboard hoặc bỏ link này. Thực ra Bác sĩ chưa có module xem tin tức nội bộ, tạm thời ẩn đi và thông báo.
                        // Sửa lại: Chuyển hướng đến bài viết phía public (vì bài viết đã PUBLISHED)
                        window.open('/news/' + data.id, '_blank');
                        tourGuideBox.classList.remove('show');
                    });

                    document.getElementById('btn-skip-alert-doc').addEventListener('click', function() {
                        localStorage.setItem('meditrust_doctor_last_alert_id', data.id.toString());
                        tourGuideBox.classList.remove('show');
                    });

                    // Ẩn Tour Guide khi mở hộp thoại chat AI
                    toggleBtn.addEventListener('click', function() {
                        if (tourGuideBox) {
                            tourGuideBox.classList.remove('show');
                        }
                    });
                }
            })
            .catch(err => console.error("Lỗi khi tải tin tức khẩn cấp (Doctor):", err));
    }

    // Delay 2s sau khi load trang rồi mới check để khỏi giật màn hình
    setTimeout(checkDoctorEmergencyAlert, 2000);

});
