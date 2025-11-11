package com.bookinghealthy.service;

import com.bookinghealthy.model.Booking;

import java.util.List;
import java.util.Optional;

public interface BookingService {
    Booking save(Booking booking);
    // (Chúng ta sẽ thêm các hàm find... sau)

    // === THÊM 3 HÀM MỚI NÀY ===
    List<Booking> findAll();
    Optional<Booking> findById(Long id);
    void deleteById(Long id);

}