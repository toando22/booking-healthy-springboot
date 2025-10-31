-- 1. Tạm thời tắt kiểm tra khóa ngoại
SET FOREIGN_KEY_CHECKS = 0;

-- 2. Xóa sạch dữ liệu cũ
TRUNCATE TABLE roles;
TRUNCATE TABLE users;
TRUNCATE TABLE user_roles;
TRUNCATE TABLE doctors;

-- 3. Bật lại kiểm tra khóa ngoại
SET FOREIGN_KEY_CHECKS = 1;

-- 4. Chèn dữ liệu mới (giữ nguyên code của bạn)
-- Chèn 3 vai trò
INSERT INTO roles(id, name) VALUES(1, 'ROLE_ADMIN');
INSERT INTO roles(id, name) VALUES(2, 'ROLE_DOCTOR');
INSERT INTO roles(id, name) VALUES(3, 'ROLE_USER');

-- Chèn dữ liệu cho bảng 'users'
-- Mật khẩu BCrypt cho "123456"
INSERT INTO users(id, username, email, password, full_name, phone, avatar)
VALUES(1, 'doctor_walter', 'walter@gmail.com', '$2a$10$MVpA8/G.pGOKd6..84S5Eeh.fS.gO.OOQvOOuNQQxX.eMhh.d/m7W', 'Walter White', '0912345678', 'doctor-1.jpg');

INSERT INTO users(id, username, email, password, full_name, phone, avatar)
VALUES(2, 'doctor_sarah', 'sarah@gmail.com', '$2a$10$MVpA8/G.pGOKd6..84S5Eeh.fS.gO.OOQvOOuNQQxX.eMhh.d/m7W', 'Sarah Connor', '0987654321', 'doctor-2.jpg');

INSERT INTO users(id, username, email, password, full_name, phone, avatar)
VALUES(3, 'patient_tom', 'tom@gmail.com', '$2a$10$MVpA8/G.pGOKd6..84S5Eeh.fS.gO.OOQvOOuNQQxX.eMhh.d/m7W', 'Tom Patient', '0900000001', NULL);

INSERT INTO users(id, username, email, password, full_name, phone, avatar)
VALUES(4, 'admin', 'admin@gmail.com', '$2a$10$MVpA8/G.pGOKd6..84S5Eeh.fS.gO.OOQvOOuNQQxX.eMhh.d/m7W', 'Adminstrator', '0900000000', NULL);


-- Liên kết user với role trong bảng 'user_roles'
INSERT INTO user_roles(user_id, role_id) VALUES(1, 2); -- User 'doctor_walter' là ROLE_DOCTOR
INSERT INTO user_roles(user_id, role_id) VALUES(2, 2); -- User 'doctor_sarah' là ROLE_DOCTOR
INSERT INTO user_roles(user_id, role_id) VALUES(3, 3); -- User 'patient_tom' là ROLE_USER
INSERT INTO user_roles(user_id, role_id) VALUES(4, 1); -- User 'admin' là ROLE_ADMIN


-- Chèn dữ liệu cho bảng 'doctors' (liên kết 1-1 với user)
INSERT INTO doctors(id, user_id, specialty, bio, experience_years)
VALUES(1, 1, 'Cardiology', 'Tiến sĩ, Bác sĩ chuyên khoa Tim mạch với hơn 15 năm kinh nghiệm tại Bệnh viện Trung ương. Chuyên điều trị các bệnh lý về tim, mạch vành.', 15);

INSERT INTO doctors(id, user_id, specialty, bio, experience_years)
VALUES(2, 2, 'Neurology', 'Thạc sĩ, Bác sĩ chuyên khoa Nội thần kinh. Có 10 năm kinh nghiệm trong chẩn đoán và điều trị đột quỵ, Parkinson và các rối loạn vận động.', 10);