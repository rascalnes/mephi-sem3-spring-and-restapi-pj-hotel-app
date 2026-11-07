package ru.nes.booking.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nes.booking.client.HotelClient;
import ru.nes.booking.dto.AvailabilityRequest;
import ru.nes.booking.dto.BookingRequest;
import ru.nes.booking.dto.BookingResponse;
import ru.nes.booking.entity.Booking;
import ru.nes.booking.entity.BookingStatus;
import ru.nes.booking.repository.BookingRepository;
import ru.nes.booking.service.BookingService;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final HotelClient hotelClient;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request, Long userId, String rawToken) {
        // 1. Определяем roomId (авто-подбор или явный)
        Long roomId = request.getRoomId();
        if (Boolean.TRUE.equals(request.getAutoSelect())) {
            roomId = hotelClient.getRecommendedRoomId(rawToken);
            log.info("Auto-selected room {} for user {}", roomId, userId);
        } else if (roomId == null) {
            throw new IllegalArgumentException("Room ID must be provided if autoSelect is false");
        }

        // 2. Создаем PENDING
        Booking booking = Booking.builder()
                .userId(userId)
                .roomId(roomId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        booking = bookingRepository.save(booking);
        log.info("Created booking {} in PENDING status", booking.getId());

        // 3. Пытаемся подтвердить
        try {
            AvailabilityRequest availReq = new AvailabilityRequest(request.getStartDate(), request.getEndDate());
            hotelClient.confirmAvailability(roomId, availReq, rawToken);

            // Успех - CONFIRMED
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            log.info("Booking {} CONFIRMED", booking.getId());

        } catch (Exception e) {
            // Ошибка - CANCELLED (откат)
            log.error("Failed to confirm availability for booking {}. Cancelling. Error: {}", booking.getId(), e.getMessage());
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            // Если это была ошибка 409 (Conflict), пробрасываем дальше, чтобы клиент знал
            if (e.getMessage() != null && e.getMessage().contains("409")) {
                throw new RuntimeException("Room is already booked for these dates");
            }
            throw new RuntimeException("Failed to create booking due to external service error");
        }

        return mapToResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getMyBookings(Long userId, Pageable pageable) {
        return bookingRepository.findAllByUserId(userId, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id, Long userId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied: not your booking");
        }
        return mapToResponse(booking);
    }

    @Override
    @Transactional
    public void cancelBooking(Long id, Long userId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied: not your booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return; // Уже отменено
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Booking {} CANCELLED by user", id);

        // В этапе 5 мы добавим сюда вызов release() для Hotel Service, если бронь была CONFIRMED
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .roomId(booking.getRoomId())
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}