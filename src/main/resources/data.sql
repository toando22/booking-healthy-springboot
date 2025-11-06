-- 1. Tạm thời tắt kiểm tra khóa ngoại
SET FOREIGN_KEY_CHECKS = 0;

-- 2. Xóa sạch dữ liệu cũ
TRUNCATE TABLE roles;
TRUNCATE TABLE users;
TRUNCATE TABLE user_roles;
TRUNCATE TABLE departments;
TRUNCATE TABLE doctors;
TRUNCATE TABLE schedules;
TRUNCATE TABLE services;
TRUNCATE TABLE bookings;

-- 3. Bật lại kiểm tra khóa ngoại
SET FOREIGN_KEY_CHECKS = 1;

-- 4. Chèn dữ liệu (KHÔNG CHÈN USER/USER_ROLES)
-- === ROLES ===
INSERT INTO roles(id, name) VALUES(1, 'ROLE_ADMIN');
INSERT INTO roles(id, name) VALUES(2, 'ROLE_DOCTOR');
INSERT INTO roles(id, name) VALUES(3, 'ROLE_USER');


-- === DEPARTMENTS (ĐÃ SỬA LỖI CÚ PHÁP) ===
INSERT INTO departments(id, name, description, image) VALUES
(1, 'Tim mạch', 'Chuyên thăm khám, chẩn đoán và điều trị các bệnh lý về tim và mạch máu.', 'dept-1.jpg'),
(2, 'Nội thần kinh', 'Chuyên điều trị đột quỵ, Parkinson và các rối loạn vận động.', 'dept-2.jpg'),
(3, 'Nhi khoa', 'Chăm sóc và điều trị các bệnh lý thường gặp ở trẻ nhỏ.', 'dept-3.jpg'),
(4, 'Da liễu', 'Điều trị các bệnh về da, mụn và dị ứng.', 'dept-4.jpg'),
(5, 'Chấn thương chỉnh hình', 'Phẫu thuật chỉnh hình và phục hồi chức năng sau tai nạn.', 'dept-5.jpg'),
(6, 'Nhãn khoa', 'Điều trị cận thị, viễn thị và các bệnh lý giác mạc.', 'dept-6.jpg'),
(7, 'Sản phụ khoa', 'Chăm sóc thai sản và điều trị vô sinh hiếm muộn.', 'dept-7.jpg'),
(8, 'Tiêu hóa', 'Điều trị viêm loét dạ dày, đại tràng và gan mật.', 'dept-8.jpg'),
(9, 'Tiết niệu', 'Điều trị sỏi thận, viêm đường tiết niệu.', 'dept-1.jpg'),
(10, 'Nội tiết', 'Điều trị bệnh tiểu đường và rối loạn hormone.', 'dept-2.jpg'),
(11, 'Ung bướu', 'Điều trị các bệnh ung thư và chăm sóc giảm nhẹ.', 'dept-3.jpg'),
(12, 'Tâm thần', 'Tư vấn và điều trị các rối loạn lo âu, trầm cảm.', 'dept-4.jpg'),
(13, 'Tai Mũi Họng', 'Điều trị viêm xoang, viêm họng mãn và ù tai.', 'dept-5.jpg'),
(14, 'Răng hàm mặt', 'Nha khoa thẩm mỹ và chỉnh nha.', 'dept-6.jpg'),
(15, 'Hô hấp', 'Điều trị bệnh hen suyễn và viêm phổi mãn tính.', 'dept-7.jpg'),
(16, 'Cơ xương khớp', 'Điều trị viêm khớp dạng thấp và thoái hóa cột sống.', 'dept-8.jpg'),
(17, 'Thận học', 'Điều trị suy thận và chạy thận nhân tạo.', 'dept-1.jpg'),
(18, 'Huyết học', 'Điều trị rối loạn đông máu và thiếu máu mãn.', 'dept-2.jpg'),
(19, 'Chẩn đoán hình ảnh', 'Phân tích phim X-quang, MRI và CT.', 'dept-3.jpg'),
(20, 'Gây mê hồi sức', 'Gây mê phẫu thuật và hồi sức sau mổ.', 'dept-4.jpg'),
(21, 'Cấp cứu', 'Xử lý các ca chấn thương và ngừng tim hô hấp.', 'dept-5.jpg'),
(22, 'Y học gia đình', 'Chăm sóc sức khỏe tổng quát, theo dõi bệnh mãn tính.', 'dept-6.jpg');


-- === DOCTORS (TẠM THỜI CHƯA CHÈN - SẼ CHÈN Ở BƯỚC SAU) ===
-- (Chúng ta sẽ chèn Doctors sau khi đã tạo Users bằng Java)


-- === SCHEDULES (TẠM THỜI CHƯA CHÈN - SẼ CHÈN Ở BƯỚC SAU) ===


-- === SERVICES (ĐÃ VIỆT HÓA) ===
INSERT INTO services(id, name, description, price, category) VALUES
(1, 'Khám tư vấn tổng quát', 'Tư vấn sức khỏe tổng quát, kiểm tra định kỳ và sàng lọc bệnh.', 300000, 'Primary'),
(2, 'Dịch vụ Tiêm chủng', 'Tiêm chủng các loại vắc-xin phòng bệnh cho trẻ em và người lớn.', 500000, 'Primary'),
(3, 'Khám chuyên khoa Tim mạch', 'Khám, chẩn đoán và điều trị các bệnh lý liên quan đến tim mạch.', 600000, 'Specialty'),
(4, 'Khám chuyên khoa Thần kinh', 'Khám, chẩn đoán và điều trị các bệnh lý về thần kinh và đột quỵ.', 600000, 'Specialty'),
(5, 'Xét nghiệm', 'Thực hiện các xét nghiệm máu, nước tiểu... để chẩn đoán bệnh.', 250000, 'Diagnostics'),
(6, 'Chẩn đoán Hình ảnh', 'Chụp X-quang, MRI, CT scanner để phát hiện các bất thường trong cơ thể.', 1200000, 'Diagnostics'),
(7, 'Cấp cứu 24/7', 'Dịch vụ cấp cứu 24/7, xử lý các trường hợp khẩn cấp, tai nạn.', 2000000, 'Emergency');