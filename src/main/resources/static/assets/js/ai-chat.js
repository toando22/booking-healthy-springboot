    document.addEventListener('DOMContentLoaded', function() {
        const widget = document.getElementById('ai-chat-widget');
        const toggleBtn = document.getElementById('ai-chat-toggle');
        const closeBtn = document.getElementById('btn-close');
        const maximizeBtn = document.getElementById('btn-maximize');
        const chatBox = document.getElementById('ai-chat-box');
        const header = document.getElementById('ai-chat-header');
        const chatInput = document.getElementById('ai-chat-input');
        const sendBtn = document.getElementById('ai-chat-send');
        const messagesContainer = document.getElementById('ai-chat-messages');

        const tabChat = document.getElementById('tab-chat');
        const tabHistory = document.getElementById('tab-history');
        const historyPanel = document.getElementById('ai-history-panel');
        const historyList = document.getElementById('history-list');

        // ==========================================
        // 1. SESSION MANAGEMENT
        // ==========================================
        let sessionId = sessionStorage.getItem('meditrust_session_id');
        if (!sessionId) {
            sessionId = 'session_' + Math.random().toString(36).substr(2, 9);
            sessionStorage.setItem('meditrust_session_id', sessionId);
        }
        // ==========================================
        // 2. LOGIC KÉO THẢ ICON & CHỐNG BUNG CHAT
        // ==========================================
        let isDraggingIcon = false;
        let hasDragged = false; // CỜ PHÂN BIỆT DRAG VS CLICK
        let iconOffsetX, iconOffsetY;
        let dragStartX = 0, dragStartY = 0;

        toggleBtn.addEventListener('mousedown', function(e) {
            dragStartX = e.clientX;
            dragStartY = e.clientY;
            hasDragged = false;

            const rect = widget.getBoundingClientRect();
            iconOffsetX = e.clientX - rect.left;
            iconOffsetY = e.clientY - rect.top;

            // KHÔNG thay đổi style ở đây
            // Chỉ đánh dấu sẵn sàng kéo
            isDraggingIcon = true;
        });

        document.addEventListener('mousemove', function(e) {
            if (!isDraggingIcon) return;

            let moveX = Math.abs(e.clientX - dragStartX);
            let moveY = Math.abs(e.clientY - dragStartY);

            // Chỉ bắt đầu kéo thật khi di chuyển > 5px
            if (moveX > 5 || moveY > 5) {
                hasDragged = true;

                // Chỉ thay đổi style lần đầu khi thực sự kéo
                widget.style.transition = 'none';
                widget.style.bottom = 'auto';
                widget.style.right = 'auto';

                let newX = e.clientX - iconOffsetX;
                let newY = e.clientY - iconOffsetY;

                if (newX < 0) newX = 0;
                if (newY < 0) newY = 0;
                if (newX + widget.offsetWidth > window.innerWidth)
                    newX = window.innerWidth - widget.offsetWidth;
                if (newY + toggleBtn.offsetHeight > window.innerHeight)
                    newY = window.innerHeight - toggleBtn.offsetHeight;

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

        // CLICK MỞ CHAT - chỉ mở khi KHÔNG kéo
        toggleBtn.addEventListener('click', function(e) {
            // Nếu vừa kéo thì bỏ qua click
            if (hasDragged) {
                hasDragged = false;
                return;
            }

            chatBox.classList.remove('d-none');
            toggleBtn.style.display = 'none';

            chatInput.focus();
            if (messagesContainer.innerHTML.trim() === '') {
                appendMessage('bot', 'Xin chào! Tôi là AI MediTrust. Bạn cần hỗ trợ vấn đề sức khỏe gì hôm nay?');
            }
        });

         // Tìm tất cả các đoạn closeBtn.addEventListener cũ và thay bằng đoạn này:
         closeBtn.addEventListener('click', (e) => {
         e.preventDefault()
             chatBox.classList.add('d-none');
             // SỬA TẠI ĐÂY: Hiện lại icon bằng flex
             toggleBtn.style.display = 'flex';
             toggleBtn.style.opacity = '1';
             toggleBtn.style.pointerEvents = 'auto';
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

    requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        });
    });
});
        // ==========================================
        // 4. KÉO THẢ DI CHUYỂN KHUNG CHAT BẰNG HEADER
        // ==========================================
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


        // ==========================================
        // 6. TABS & HISTORY
        // ==========================================
        tabChat.addEventListener('click', () => {
            tabChat.classList.add('active'); tabHistory.classList.remove('active');
            historyPanel.style.display = 'none';
        });

        tabHistory.addEventListener('click', async () => {
            tabHistory.classList.add('active'); tabChat.classList.remove('active');
            historyPanel.style.display = 'block';

            try {
                const res = await fetch('/api/chat/history');
                if (res.ok) {
                    const data = await res.json();
                    if(data.length === 0) {
                        historyList.innerHTML = '<div style="text-align:center; color:#888; margin-top:20px;">Bạn chưa có lịch sử tư vấn nào hoặc chưa đăng nhập.</div>';
                        document.getElementById('history-loading').style.display = 'none';
                        return;
                    }

                    let html = '';
                    data.forEach(item => {
                        const dateStr = new Date(item.date).toLocaleString('vi-VN');
                        let previewText = "Phiên tư vấn sức khỏe";
                        try {
                            const parsedChat = JSON.parse(item.chatData);
                            const firstUserMsg = parsedChat.find(m => m.role === 'user');
                            if(firstUserMsg) previewText = firstUserMsg.content;
                        } catch(e) {}

                        html += `
                            <div class="history-item" data-chat='${item.chatData.replace(/'/g, "&#39;")}'>
                                <div class="history-date"><i class="bi bi-clock-history"></i> ${dateStr}</div>
                                <div class="history-preview"><b>Hỏi:</b> ${previewText}</div>
                            </div>
                        `;
                    });
                    historyList.innerHTML = html;
                    document.getElementById('history-loading').style.display = 'none';

                    document.querySelectorAll('.history-item').forEach(el => {
                        el.addEventListener('click', function() {
                            const rawData = this.getAttribute('data-chat');
                            const chatArray = JSON.parse(rawData);
                            messagesContainer.innerHTML = '';
                            chatArray.forEach(msg => {
                                if(msg.role !== 'system') appendMessage(msg.role, msg.content);
                            });
                            tabChat.click();
                        });
                    });
                }
            } catch(e) { console.error(e); }
        });

        // ==========================================
        // 7. SEND MESSAGE & GENERATIVE UI
        // ==========================================
        function appendMessage(sender, htmlContent) {
           const msgDiv = document.createElement('div');
                   msgDiv.classList.add('chat-msg', sender === 'assistant' || sender === 'bot' ? 'bot' : 'user');
                   msgDiv.innerHTML = htmlContent;
                   messagesContainer.appendChild(msgDiv);
                   messagesContainer.scrollTop = messagesContainer.scrollHeight;

                   // Lưu toàn bộ nội dung HTML của khung chat vào Session ngay lập tức
                   sessionStorage.setItem('meditrust_chat_html', messagesContainer.innerHTML);
                   return msgDiv;
        }

        async function sendMessage() {
            const text = chatInput.value.trim();
            if (!text) return;
            appendMessage('user', text);
            chatInput.value = '';

            const typingMsg = appendMessage('bot', '<span class="typing-indicator">Đang phân tích...</span>');

            try {
                const response = await fetch('/api/chat/ask', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ sessionId: sessionId, prompt: text })
                });

                if (response.ok) {
                    const data = await response.json();
                    let aiText = data.answer;

                   // TÌM VÀ THAY THẾ TOÀN BỘ ĐOẠN if (match) BẰNG ĐOẠN DƯỚI ĐÂY:
                                   let actionHtml = '';
                                   const match = aiText.match(/\[BOOK_DEPT_(\d+)\]/);
                                   if (match) {
                                       const deptId = match[1];
                                       aiText = aiText.replace(match[0], '');
                                       try {

                                             //const docRes = await fetch(`/api/chat/doctors/department/${deptId}`);

                                             //chặn lịch trong 3 phút
                                             const docRes = await fetch(`/api/chat/doctors/department/${deptId}?sessionId=${sessionId}`);
                                           if (docRes.ok) {
                                               const doctors = await docRes.json();
                                               if (doctors.length > 0) {
                                                  // actionHtml = `<div class="mt-3">
                                                  //     <p class="mb-2" style="font-size: 13px; font-weight: bold; color: #0d6efd;">
                                                  //         <i class="bi bi-person-badge"></i> Bác sĩ chuyên khoa đang sẵn sàng:
                                                  //     </p>
                                                  //     <div style="display: flex; gap: 12px; overflow-x: auto; padding-bottom: 10px; scroll-snap-type: x mandatory; -webkit-overflow-scrolling: touch;">`;

                                                  actionHtml = `<div class="mt-3">
                                                                                          <div style="background: #fff3cd; color: #856404; padding: 6px 10px; border-radius: 5px; font-size: 11px; font-weight: bold; margin-bottom: 10px; border-left: 3px solid #ffeeba; display: flex; align-items: center; gap: 5px;">
                                                                                              <i class="bi bi-hourglass-split" style="animation: spin 2s linear infinite;"></i>
                                                                                              Hệ thống đang tạm giữ lịch trống trong 3 phút. Hãy chọn nhanh!
                                                                                          </div>
                                                                                          <p class="mb-2" style="font-size: 13px; font-weight: bold; color: #0d6efd;">
                                                                                              <i class="bi bi-person-badge"></i> Bác sĩ chuyên khoa đang sẵn sàng:
                                                                                          </p>
                                                                                          <div style="display: flex; gap: 12px; overflow-x: auto; padding-bottom: 10px; scroll-snap-type: x mandatory; -webkit-overflow-scrolling: touch;">`;
                                                   for (const doc of doctors) {
                                                   let slotsHtml = `
                                                                                           <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                                                                                               <span style="font-size: 11px; color: #888; font-weight: 600;">Ca trống gần nhất:</span>
                                                                                               <a href="/appointment?doctorId=${doc.id}"
                                                                                                  style="font-size: 10px; color: #198754; text-decoration: none; font-weight: bold; background: #e8f5e9; padding: 3px 8px; border-radius: 12px; transition: all 0.2s;"
                                                                                                  onmouseover="this.style.background='#198754'; this.style.color='white';"
                                                                                                  onmouseout="this.style.background='#e8f5e9'; this.style.color='#198754';">
                                                                                                  <i class="bi bi-calendar-plus"></i> Chọn lịch khác
                                                                                               </a>
                                                                                           </div>
                                                                                       `;

                                                       if (doc.availableSlots && doc.availableSlots.length > 0) {
                                                           slotsHtml += doc.availableSlots.map(time =>
                                                               `<a href="/appointment?doctorId=${doc.id}"
                                                                   style="display: inline-block; padding: 4px 8px; margin: 2px; border: 1px solid #0d6efd; color: #0d6efd; border-radius: 5px; text-decoration: none; font-size: 11px; font-weight: 500; transition: all 0.2s;"
                                                                   onmouseover="this.style.background='#0d6efd'; this.style.color='white';"
                                                                   onmouseout="this.style.background='white'; this.style.color='#0d6efd';">
                                                                   ${time}
                                                               </a>`
                                                           ).join('');
                                                       } else {
                                                           slotsHtml += `<span style="font-size: 11px; color: #dc3545;">Tạm hết lịch trực</span>`;
                                                       }

                                                       actionHtml += `
                                                           <div style="background: #fff; border: 1px solid #e0e0e0; border-radius: 10px; padding: 12px; min-width: 260px; scroll-snap-align: start; flex-shrink: 0; box-shadow: 0 2px 4px rgba(0,0,0,0.05);">
                                                               <div style="display: flex; align-items: center; margin-bottom: 10px;">
                                                                   <img src="${doc.avatar}" onerror="this.src='/assets/img/default-doctor.png'"
                                                                        style="width: 45px; height: 45px; border-radius: 50%; object-fit: cover; border: 2px solid #f8f9fa; margin-right: 12px;">
                                                                   <div>
                                                                       <div style="font-size: 14px; font-weight: bold; color: #333;">${doc.fullName}</div>
                                                                       <div style="font-size: 12px; color: #666;">${doc.degree} • ${doc.experienceYears} năm KN</div>
                                                                       <div style="font-size: 12px; color: #ffc107;">⭐⭐⭐⭐⭐ 5.0</div>
                                                                   </div>
                                                               </div>
                                                               <div style="border-top: 1px dashed #eee; padding-top: 8px;">
                                                                   ${slotsHtml}
                                                               </div>
                                                           </div>
                                                       `;
                                                   }

                                                   actionHtml += `
                                                       <div style="min-width: 120px; display: flex; align-items: center; justify-content: center; scroll-snap-align: start; flex-shrink: 0;">
                                                           <a href="/doctors?departmentId=${deptId}" style="text-align: center; color: #0d6efd; text-decoration: none; font-weight: bold; font-size: 13px;">
                                                               <div style="width: 40px; height: 40px; border-radius: 50%; background: #e9ecef; display: flex; align-items: center; justify-content: center; margin: 0 auto 5px;">
                                                                   <i class="bi bi-arrow-right"></i>
                                                               </div>
                                                               Xem tất cả
                                                           </a>
                                                       </div>
                                                   </div></div>`;
                                               } else {
                                                   actionHtml = `<div style="margin-top: 15px;"><a href="/appointment?departmentId=${deptId}" style="background: #198754; color: white; padding: 8px 15px; border-radius: 20px; text-decoration: none; font-size: 13px; font-weight: bold;"><i class="bi bi-calendar2-check"></i> Đặt lịch Khoa này ngay</a></div>`;
                                               }
                                           }
                                       } catch (err) { console.error(err); }
                                   }

                    typingMsg.innerHTML = aiText.replace(/\n/g, '<br>') + actionHtml;

                    // THÊM DÒNG NÀY:
                    sessionStorage.setItem('meditrust_chat_html', messagesContainer.innerHTML);
                } else {
                    typingMsg.innerHTML = 'Hệ thống bận.';
                }
            } catch (error) {
                typingMsg.innerHTML = 'Lỗi kết nối.';
            }
        }

        sendBtn.addEventListener('click', sendMessage);
        chatInput.addEventListener('keypress', (e) => { if (e.key === 'Enter') sendMessage(); });
    });