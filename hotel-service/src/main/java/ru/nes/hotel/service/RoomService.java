package ru.nes.hotel.service;

import ru.nes.hotel.dto.AvailabilityRequest;
import ru.nes.hotel.dto.RoomRequest;
import ru.nes.hotel.dto.RoomResponse;
import java.util.List;

public interface RoomService {
    RoomResponse createRoom(RoomRequest request);
    List<RoomResponse> getAllAvailableRooms();
    List<RoomResponse> getRecommendedRooms();
    void confirmAvailability(Long roomId, AvailabilityRequest request);
    void release(Long roomId, AvailabilityRequest request);
}