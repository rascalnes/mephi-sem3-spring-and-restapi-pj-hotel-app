package ru.nes.booking.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.nes.booking.dto.BookingRequest;
import ru.nes.booking.dto.BookingResponse;

public interface BookingService {
    BookingResponse createBooking(BookingRequest request, Long userId, String rawToken);
    Page<BookingResponse> getMyBookings(Long userId, Pageable pageable);
    BookingResponse getBookingById(Long id, Long userId);
    void cancelBooking(Long id, Long userId);
}