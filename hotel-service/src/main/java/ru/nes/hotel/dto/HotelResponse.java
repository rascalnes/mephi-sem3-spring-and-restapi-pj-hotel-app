package ru.nes.hotel.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HotelResponse {
    private Long id;
    private String name;
    private String address;
}