package ru.nes.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HotelRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String address;
}