package com.bookinghealthy.config;

import com.bookinghealthy.model.*;
import com.bookinghealthy.repository.DepartmentRepository;
import com.bookinghealthy.repository.DoctorRepository;
import com.bookinghealthy.repository.RoleRepository;
import com.bookinghealthy.repository.UserRepository;
import com.bookinghealthy.repository.ScheduleRepository; // Thêm import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek; // Thêm import
import java.time.LocalTime; // Thêm import
import java.util.List; // Thêm import
import java.util.Map; // Thêm import
import java.util.Set; // Thêm import
import java.util.stream.Collectors; // Thêm import

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private DepartmentRepository departmentRepository; // Thêm
    @Autowired private DoctorRepository doctorRepository; // Thêm
    @Autowired private ScheduleRepository scheduleRepository; // Thêm

    @Override
    public void run(String... args) throws Exception {

        // Chỉ chạy nếu bảng 'users' rỗng
        if (userRepository.count() == 0) {

            System.out.println(">>> ĐANG KHỞI TẠO DỮ LIỆU BẰNG JAVA...");

            // 1. Lấy Roles (đã được data.sql chèn)
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
            Role doctorRole = roleRepository.findByName("ROLE_DOCTOR").orElseThrow();
            Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();

            // Lấy mật khẩu
            String pass123 = passwordEncoder.encode("123456");
            String passABC = passwordEncoder.encode("abc123!@#");

            // 2. Tạo Users (Admin, Patient, 22 Bác sĩ)
            User admin = new User(null, "admin", "admin@gmail.com", pass123, "Administrator", "0900000000", null, Set.of(adminRole));
            User patientTom = new User(null, "patient_tom", "tom@gmail.com", pass123, "Tom Patient", "0900000001", null, Set.of(userRole));
            User testSang = new User(null, "testsang31", "testsang31@gmail.com", passABC, "Test Sang 31", "0900000002", null, Set.of(userRole));

            List<User> doctorUsers = List.of(
                    new User(null, "doctor_walter", "walter@gmail.com", pass123, "Walter White", "0912345678", "doctor-1.jpg", Set.of(doctorRole)),
                    new User(null, "doctor_sarah", "sarah@gmail.com", pass123, "Sarah Connor", "0987654321", "doctor-2.jpg", Set.of(doctorRole)),
                    new User(null, "bs_thuha", "thuha.nguyen@example.com", pass123, "Nguyễn Thị Thu Hà", "0905123456", "doctor-3.jpg", Set.of(doctorRole)),
                    new User(null, "bs_quangdung", "quangdung.tran@example.com", pass123, "Trần Quang Dũng", "0978123456", "doctor-4.jpg", Set.of(doctorRole)),
                    new User(null, "bs_minhtuan", "minhtuan.le@example.com", pass123, "Lê Minh Tuấn", "0912349876", "doctor-5.jpg", Set.of(doctorRole)),
                    new User(null, "bs_hongnhung", "hongnhung.pham@example.com", pass123, "Phạm Hồng Nhung", "0932123456", "doctor-6.jpg", Set.of(doctorRole)),
                    new User(null, "bs_lananh", "lananh.do@example.com", pass123, "Đỗ Thị Lan Anh", "0988123123", "doctor-7.jpg", Set.of(doctorRole)),
                    new User(null, "bs_vanquan", "vanquan.nguyen@example.com", pass123, "Nguyễn Văn Quân", "0966668888", "doctor-8.jpg", Set.of(doctorRole)),
                    new User(null, "bs_congphu", "congphu.truong@example.com", pass123, "Trương Công Phú", "0912445566", "doctor-9.jpg", Set.of(doctorRole)),
                    new User(null, "bs_luuthimai", "luuthimai@example.com", pass123, "Lưu Thị Mai", "0977554433", "doctor-10.jpg", Set.of(doctorRole)),
                    new User(null, "bs_huuduc", "huuduc.phan@example.com", pass123, "Phan Hữu Đức", "0912333444", "doctor-11.jpg", Set.of(doctorRole)),
                    new User(null, "bs_kimlien", "kimlien.vu@example.com", pass123, "Vũ Thị Kim Liên", "0908222333", "doctor-12.jpg", Set.of(doctorRole)),
                    new User(null, "bs_huukhanh", "huukhanh.nguyen@example.com", pass123, "Nguyễn Hữu Khánh", "0977445566", "doctor-13.jpg", Set.of(doctorRole)),
                    new User(null, "bs_thaodang", "thaodang@example.com", pass123, "Đặng Thanh Thảo", "0912999888", "doctor-14.jpg", Set.of(doctorRole)),
                    new User(null, "bs_quoccuong", "quoccuong.bui@example.com", pass123, "Bùi Quốc Cường", "0935667788", "doctor-15.jpg", Set.of(doctorRole)),
                    new User(null, "bs_thihanh", "thihanh.nguyen@example.com", pass123, "Nguyễn Thị Hạnh", "0988665544", "doctor-16.jpg", Set.of(doctorRole)),
                    new User(null, "bs_vanhau", "vanhau.pham@example.com", pass123, "Phạm Văn Hậu", "0909777555", "doctor-17.jpg", Set.of(doctorRole)),
                    new User(null, "bs_baongoc", "baongoc.tran@example.com", pass123, "Trần Bảo Ngọc", "0932111777", "doctor-18.jpg", Set.of(doctorRole)),
                    new User(null, "bs_vanson", "vanson.doan@example.com", pass123, "Đoàn Văn Sơn", "0977000111", "doctor-19.jpg", Set.of(doctorRole)),
                    new User(null, "bs_hothihuong", "hothihuong@example.com", pass123, "Hồ Thị Hương", "0918000222", "doctor-20.jpg", Set.of(doctorRole)),
                    new User(null, "bs_lamanhdung", "lamanhdung@example.com", pass123, "Lâm Anh Dũng", "0933444555", "doctor-21.jpg", Set.of(doctorRole)),
                    new User(null, "bs_thanhtam", "thanhtam.nguyen@example.com", pass123, "Nguyễn Thanh Tâm", "0909666777", "doctor-22.jpg", Set.of(doctorRole))
            );

            userRepository.save(admin);
            userRepository.save(patientTom);
            userRepository.save(testSang);
            List<User> savedDoctorUsers = userRepository.saveAll(doctorUsers);

            // 3. Lấy Departments (đã được data.sql chèn)
            Map<String, Department> departmentsMap = departmentRepository.findAll().stream()
                    .collect(Collectors.toMap(Department::getName, dept -> dept));

            // 4. Tạo Doctors (Liên kết User và Department)
            List<Doctor> doctors = List.of(
                    new Doctor(null, savedDoctorUsers.get(0), departmentsMap.get("Tim mạch"), "Tiến sĩ, Bác sĩ chuyên khoa Tim mạch...", 15),
                    new Doctor(null, savedDoctorUsers.get(1), departmentsMap.get("Nội thần kinh"), "Thạc sĩ, Bác sĩ chuyên khoa Nội thần kinh...", 10),
                    new Doctor(null, savedDoctorUsers.get(2), departmentsMap.get("Nhi khoa"), "Bác sĩ Nguyễn Thị Thu Hà – chuyên khoa Nhi...", 12),
                    new Doctor(null, savedDoctorUsers.get(3), departmentsMap.get("Da liễu"), "Bác sĩ Trần Quang Dũng – chuyên khoa Da liễu...", 9),
                    new Doctor(null, savedDoctorUsers.get(4), departmentsMap.get("Chấn thương chỉnh hình"), "Bác sĩ Lê Minh Tuấn – hơn 15 năm kinh nghiệm...", 15),
                    new Doctor(null, savedDoctorUsers.get(5), departmentsMap.get("Nhãn khoa"), "Bác sĩ Phạm Hồng Nhung – chuyên khoa Mắt...", 10),
                    new Doctor(null, savedDoctorUsers.get(6), departmentsMap.get("Sản phụ khoa"), "Bác sĩ Đỗ Thị Lan Anh – hơn 14 năm kinh nghiệm...", 14),
                    new Doctor(null, savedDoctorUsers.get(7), departmentsMap.get("Tiêu hóa"), "Bác sĩ Nguyễn Văn Quân – chuyên khoa Tiêu hóa...", 13),
                    new Doctor(null, savedDoctorUsers.get(8), departmentsMap.get("Tiết niệu"), "Bác sĩ Trương Công Phú – chuyên khoa Tiết niệu...", 8),
                    new Doctor(null, savedDoctorUsers.get(9), departmentsMap.get("Nội tiết"), "Bác sĩ Lưu Thị Mai – hơn 16 năm công tác...", 16),
                    new Doctor(null, savedDoctorUsers.get(10), departmentsMap.get("Ung bướu"), "Bác sĩ Phan Hữu Đức – chuyên khoa Ung bướu...", 18),
                    new Doctor(null, savedDoctorUsers.get(11), departmentsMap.get("Tâm thần"), "Bác sĩ Vũ Thị Kim Liên – hơn 11 năm kinh nghiệm...", 11),
                    new Doctor(null, savedDoctorUsers.get(12), departmentsMap.get("Tai Mũi Họng"), "Bác sĩ Nguyễn Hữu Khánh – chuyên khoa Tai Mũi Họng...", 9),
                    new Doctor(null, savedDoctorUsers.get(13), departmentsMap.get("Răng hàm mặt"), "Bác sĩ Đặng Thanh Thảo – hơn 7 năm kinh nghiệm...", 7),
                    new Doctor(null, savedDoctorUsers.get(14), departmentsMap.get("Hô hấp"), "Bác sĩ Bùi Quốc Cường – chuyên khoa Hô hấp...", 13),
                    new Doctor(null, savedDoctorUsers.get(15), departmentsMap.get("Cơ xương khớp"), "Bác sĩ Nguyễn Thị Hạnh – hơn 10 năm kinh nghiệm...", 10),
                    new Doctor(null, savedDoctorUsers.get(16), departmentsMap.get("Thận học"), "Bác sĩ Phạm Văn Hậu – chuyên khoa Thận học...", 9),
                    new Doctor(null, savedDoctorUsers.get(17), departmentsMap.get("Huyết học"), "Bác sĩ Trần Bảo Ngọc – chuyên khoa Huyết học...", 15),
                    new Doctor(null, savedDoctorUsers.get(18), departmentsMap.get("Chẩn đoán hình ảnh"), "Bác sĩ Đoàn Văn Sơn – hơn 12 năm kinh nghiệm...", 12),
                    new Doctor(null, savedDoctorUsers.get(19), departmentsMap.get("Gây mê hồi sức"), "Bác sĩ Hồ Thị Hương – hơn 13 năm kinh nghiệm...", 13),
                    new Doctor(null, savedDoctorUsers.get(20), departmentsMap.get("Cấp cứu"), "Bác sĩ Lâm Anh Dũng – chuyên khoa Cấp cứu...", 10),
                    new Doctor(null, savedDoctorUsers.get(21), departmentsMap.get("Y học gia đình"), "Bác sĩ Nguyễn Thanh Tâm – hơn 11 năm kinh nghiệm...", 11)
            );
            List<Doctor> savedDoctors = doctorRepository.saveAll(doctors);

            // 5. Tạo Lịch làm việc (Schedules)
            Schedule s1 = new Schedule(null, savedDoctors.get(0), DayOfWeek.MONDAY, LocalTime.parse("08:00:00"), LocalTime.parse("11:00:00"));
            Schedule s2 = new Schedule(null, savedDoctors.get(0), DayOfWeek.TUESDAY, LocalTime.parse("08:00:00"), LocalTime.parse("11:00:00"));
            Schedule s3 = new Schedule(null, savedDoctors.get(0), DayOfWeek.THURSDAY, LocalTime.parse("14:00:00"), LocalTime.parse("16:00:00"));
            Schedule s4 = new Schedule(null, savedDoctors.get(2), DayOfWeek.TUESDAY, LocalTime.parse("08:00:00"), LocalTime.parse("12:00:00"));
            Schedule s5 = new Schedule(null, savedDoctors.get(2), DayOfWeek.TUESDAY, LocalTime.parse("13:30:00"), LocalTime.parse("16:30:00"));

            scheduleRepository.saveAll(List.of(s1, s2, s3, s4, s5));

            System.out.println(">>> KHỞI TẠO DỮ LIỆU JAVA HOÀN TẤT <<<");
        }
    }
}