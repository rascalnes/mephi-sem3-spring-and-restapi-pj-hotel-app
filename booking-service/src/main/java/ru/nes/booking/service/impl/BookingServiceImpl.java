package ru.nes.booking.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import ru.nes.booking.client.HotelClient;
import ru.nes.booking.dto.AvailabilityRequest;
import ru.nes.booking.dto.BookingRequest;
import ru.nes.booking.dto.BookingResponse;
import ru.nes.booking.entity.Booking;
import ru.nes.booking.entity.BookingStatus;
import ru.nes.booking.exception.ConflictException;
import ru.nes.booking.repository.BookingRepository;
import ru.nes.booking.service.BookingService;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final HotelClient hotelClient;

    @Override
    public BookingResponse createBooking(BookingRequest request, Long userId, String rawToken) {
        Long roomId = request.getRoomId();
        if (Boolean.TRUE.equals(request.getAutoSelect())) {
            roomId = hotelClient.getRecommendedRoomId(rawToken);
            log.info("Auto-selected room {} for user {}", roomId, userId);
        } else if (roomId == null) {
            throw new IllegalArgumentException("Room ID must be provided if autoSelect is false");
        }

        // 1. Локальная транзакция: Создаем PENDING
        Booking booking = saveBooking(userId, roomId, request, BookingStatus.PENDING);

        // Добавляем bookingId в MDC для сквозного логирования
        MDC.put("bookingId", booking.getId().toString());

        try {
            log.info("Saga started for booking {}. Status: PENDING", booking.getId());

            AvailabilityRequest availReq = new AvailabilityRequest(request.getStartDate(), request.getEndDate());

            // 2. Удаленный вызов с Timeout (TimeLimiter ограничен 2 секундами)
            CompletableFuture<Void> future = hotelClient.confirmAvailability(roomId, availReq, rawToken);
            future.get(3, TimeUnit.SECONDS); // Ждем чуть больше, чем TimeLimiter

            // 3. Успех: CONFIRMED
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            log.info("Saga completed for booking {}. Status: CONFIRMED", booking.getId());

        } catch (Exception e) {
            // 4. Ошибка: CANCELLED + компенсация
            log.error("Saga failed for booking {}. Executing compensation...", booking.getId());

            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            log.info("Booking {} status changed to CANCELLED", booking.getId());

            // Компенсация: Release слота
            try {
                AvailabilityRequest availReq = new AvailabilityRequest(request.getStartDate(), request.getEndDate());
                hotelClient.release(roomId, availReq, rawToken);
                log.info("Compensation successful: released room {} for booking {}", roomId, booking.getId());
            } catch (Exception releaseEx) {
                log.error("Compensation failed (best-effort): {}", releaseEx.getMessage());
            }

            // Пробрасываем ошибку наверх для клиента
            if (e.getCause() instanceof HttpClientErrorException.Conflict || e instanceof HttpClientErrorException.Conflict) {
                throw new ConflictException("Room is already booked for these dates");
            }
            throw new RuntimeException("Failed to create booking due to external service error: " + e.getMessage());
        } finally {
            MDC.remove("bookingId");
        }

        return mapToResponse(booking);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Booking saveBooking(Long userId, Long roomId, BookingRequest request, BookingStatus status) {
        Booking booking = Booking.builder()
                .userId(userId)
                .roomId(roomId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
        return bookingRepository.saveAndFlush(booking);
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
            return;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Booking {} CANCELLED by user", id);
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