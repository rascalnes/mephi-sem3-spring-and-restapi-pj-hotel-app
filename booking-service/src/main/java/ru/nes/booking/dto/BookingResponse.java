package ru.nes.booking.dto;

import lombok.Builder;
import lombok.Data;
import ru.nes.booking.entity.BookingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponse {
    private Long id;
    private Long roomId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BookingStatus status;
    private LocalDateTime createdAt;
}