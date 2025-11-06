package com.bookinghealthy.service.impl;

import com.bookinghealthy.model.Booking;
import com.bookinghealthy.repository.BookingRepository;
import com.bookinghealthy.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Override
    public Booking save(Booking booking) {
        return bookingRepository.save(booking);
    }
}