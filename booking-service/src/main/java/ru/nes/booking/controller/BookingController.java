package ru.nes.booking.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import ru.nes.booking.dto.BookingRequest;
import ru.nes.booking.dto.BookingResponse;
import ru.nes.booking.entity.ProcessedRequest;
import ru.nes.booking.repository.ProcessedRequestRepository;
import ru.nes.booking.service.BookingService;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final ProcessedRequestRepository processedRequestRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/booking")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BookingResponse> createBooking(
            @RequestHeader("Idempotency-Key") String requestId,
            @Valid @RequestBody BookingRequest request,
            JwtAuthenticationToken token) throws JsonProcessingException {

        // Проверка идемпотентности
        ProcessedRequest existing = processedRequestRepository.findById(requestId).orElse(null);
        if (existing != null) {
            BookingResponse cachedResponse = objectMapper.readValue(existing.getResponseBody(), BookingResponse.class);
            return ResponseEntity.status(existing.getStatusCode()).body(cachedResponse);
        }

        // Извлечь userId из токена
        Long userId = ((Number) token.getToken().getClaim("userId")).longValue();
        String rawToken = "Bearer " + token.getToken().getTokenValue();

        // Обработка
        BookingResponse response = bookingService.createBooking(request, userId, rawToken);

        // Сохранение ответа для идемпотентности
        ProcessedRequest pr = ProcessedRequest.builder()
                .requestId(requestId)
                .responseBody(objectMapper.writeValueAsString(response))
                .statusCode(HttpStatus.CREATED.value())
                .createdAt(LocalDateTime.now())
                .build();
        processedRequestRepository.save(pr);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/bookings") // Путь /bookings
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Page<BookingResponse> getMyBookings(
            Pageable pageable,
            JwtAuthenticationToken token) {
        Long userId = ((Number) token.getToken().getClaim("userId")).longValue();
        return bookingService.getMyBookings(userId, pageable);
    }

    @GetMapping("/booking/{id}") // Путь /booking/{id}
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public BookingResponse getBookingById(
            @PathVariable Long id,
            JwtAuthenticationToken token) {
        Long userId = ((Number) token.getToken().getClaim("userId")).longValue();
        return bookingService.getBookingById(id, userId);
    }

    @DeleteMapping("/booking/{id}") // Путь /booking/{id}
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void cancelBooking(
            @PathVariable Long id,
            JwtAuthenticationToken token) {
        Long userId = ((Number) token.getToken().getClaim("userId")).longValue();
        bookingService.cancelBooking(id, userId);
    }
}