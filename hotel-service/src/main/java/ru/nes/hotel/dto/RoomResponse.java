package ru.nes.hotel.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomResponse {
    private Long id;
    private String number;
    private Long hotelId;
    private Boolean available;
    private Integer timesBooked;
}