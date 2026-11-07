package ru.nes.hotel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class AvailabilityRequest {
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
}