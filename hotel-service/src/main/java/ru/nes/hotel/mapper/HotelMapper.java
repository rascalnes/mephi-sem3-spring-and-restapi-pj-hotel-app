package ru.nes.hotel.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.nes.hotel.dto.HotelRequest;
import ru.nes.hotel.dto.HotelResponse;
import ru.nes.hotel.dto.RoomRequest;
import ru.nes.hotel.dto.RoomResponse;
import ru.nes.hotel.entity.Hotel;
import ru.nes.hotel.entity.Room;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface HotelMapper {
    Hotel toEntity(HotelRequest request);
    HotelResponse toDto(Hotel hotel);

    Room toEntity(RoomRequest request);
    RoomResponse toDto(Room room);
}