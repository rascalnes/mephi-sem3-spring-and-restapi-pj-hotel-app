package ru.nes.hotel.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nes.hotel.dto.HotelRequest;
import ru.nes.hotel.dto.HotelResponse;
import ru.nes.hotel.entity.Hotel;
import ru.nes.hotel.mapper.HotelMapper;
import ru.nes.hotel.repository.HotelRepository;
import ru.nes.hotel.service.HotelService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final HotelMapper hotelMapper;

    @Override
    @Transactional
    public HotelResponse createHotel(HotelRequest request) {
        Hotel hotel = hotelMapper.toEntity(request);
        return hotelMapper.toDto(hotelRepository.save(hotel));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelResponse> getAllHotels() {
        return hotelRepository.findAll().stream()
                .map(hotelMapper::toDto)
                .collect(Collectors.toList());
    }
}