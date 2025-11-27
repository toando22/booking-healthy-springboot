package com.bookinghealthy.controller.doctor;

import com.bookinghealthy.model.Booking;
import com.bookinghealthy.model.BookingStatus;
import com.bookinghealthy.model.Doctor;
import com.bookinghealthy.repository.BookingRepository;
import com.bookinghealthy.service.DoctorService;
import com.bookinghealthy.service.MedicalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/doctor/medical-record")
public class DoctorMedicalRecordController {

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private DoctorService doctorService;

    // Helper: Lấy bác sĩ đang đăng nhập
    private Doctor getLoggedInDoctor(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return doctorService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
    }

    // 1. HIỂN THỊ FORM KHÁM BỆNH
    @GetMapping("/create/{bookingId}")
    public String showCreateForm(@PathVariable("bookingId") Long bookingId,
                                 Model model,
                                 Authentication authentication,
                                 RedirectAttributes ra) {
        Doctor currentDoctor = getLoggedInDoctor(authentication);

        // Tìm Booking và validate quyền
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Check 1: Lịch này có phải của Bác sĩ này không?
        if (!booking.getDoctor().getId().equals(currentDoctor.getId())) {
            ra.addFlashAttribute("errorMessage", "Bạn không có quyền khám ca này!");
            return "redirect:/doctor/examinations";
        }

        // Check 2: Lịch này có phải trạng thái CONFIRMED không? (Chỉ Confirmed mới được khám)
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            ra.addFlashAttribute("errorMessage", "Lịch hẹn không hợp lệ để khám (Phải ở trạng thái Đã xác nhận).");
            return "redirect:/doctor/examinations";
        }

        model.addAttribute("booking", booking);
        return "doctor/medical-record-form";
    }

    // 2. XỬ LÝ LƯU BỆNH ÁN (POST)
    @PostMapping("/save")
    public String saveMedicalRecord(
            @RequestParam("bookingId") Long bookingId,
            @RequestParam("symptoms") String symptoms,
            @RequestParam("diagnosis") String diagnosis,
            @RequestParam("prescription") String prescription,
            @RequestParam("doctorNotes") String doctorNotes,
            Authentication authentication,
            RedirectAttributes ra) {

        try {
            // Gọi Service để lưu bệnh án + Update status Booking -> COMPLETED
            medicalRecordService.createMedicalRecord(bookingId, symptoms, diagnosis, prescription, doctorNotes);

            ra.addFlashAttribute("successMessage", "Đã hoàn tất ca khám và lưu hồ sơ bệnh án thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("errorMessage", "Lỗi khi lưu bệnh án: " + e.getMessage());
            return "redirect:/doctor/medical-record/create/" + bookingId; // Quay lại form nếu lỗi
        }

        return "redirect:/doctor/examinations"; // Quay về danh sách khám
    }
    // 3. XEM LẠI BỆNH ÁN (Dành cho Bác sĩ)
    @GetMapping("/view/{bookingId}")
    public String viewRecordForDoctor(@PathVariable("bookingId") Long bookingId,
                                      Model model,
                                      Authentication authentication,
                                      RedirectAttributes ra) {
        Doctor currentDoctor = getLoggedInDoctor(authentication);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Check quyền: Bác sĩ xem phải là người khám ca này
        if (!booking.getDoctor().getId().equals(currentDoctor.getId())) {
            ra.addFlashAttribute("errorMessage", "Bạn không có quyền xem hồ sơ này.");
            return "redirect:/doctor/examinations";
        }

        // Tìm bệnh án
        // Lưu ý: Cần Inject MedicalRecordRepository vào Controller này nếu chưa có
        // @Autowired private MedicalRecordRepository medicalRecordRepository;
        var record = medicalRecordService.findByBookingId(bookingId).orElse(null);

        if (record == null) {
            ra.addFlashAttribute("errorMessage", "Hồ sơ bệnh án chưa tồn tại.");
            return "redirect:/doctor/examinations";
        }

        model.addAttribute("record", record);
        model.addAttribute("booking", booking);

        // === THÊM 2 DÒNG NÀY ===
        model.addAttribute("role", "DOCTOR"); // Đánh dấu người xem là Bác sĩ
        model.addAttribute("backLink", "/doctor/examinations"); // Quay lại trang danh sách khám

        // Tái sử dụng giao diện xem chi tiết của User (nhưng cần chỉnh sửa chút xíu ở View để ẩn nút quay lại của User)
        return "user/medical-record-detail";
    }
}