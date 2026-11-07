package ru.nes.booking.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingRequest {
    @NotNull
    private Boolean autoSelect;

    private Long roomId; // Игнорируется, если autoSelect = true

    @NotNull
    @FutureOrPresent
    private LocalDate startDate;

    @NotNull
    @FutureOrPresent
    private LocalDate endDate;

    @AssertTrue(message = "startDate must be before endDate")
    public boolean isDatesValid() {
        if (startDate == null || endDate == null) return true; // Обрабатывается @NotNull
        return startDate.isBefore(endDate);
    }
}