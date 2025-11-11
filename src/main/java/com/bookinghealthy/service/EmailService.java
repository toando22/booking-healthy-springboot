package com.bookinghealthy.service;

import com.bookinghealthy.model.Booking;

public interface EmailService {
    void sendBookingConfirmation(Booking booking);

    // === THÊM HÀM MỚI NÀY ===
    void sendBookingCancellation(Booking booking, String reason); // Thêm "reason" (lý do)
}