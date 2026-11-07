package ru.nes.hotel.service;

import ru.nes.hotel.dto.HotelRequest;
import ru.nes.hotel.dto.HotelResponse;
import java.util.List;

public interface HotelService {
    HotelResponse createHotel(HotelRequest request);
    List<HotelResponse> getAllHotels();
}