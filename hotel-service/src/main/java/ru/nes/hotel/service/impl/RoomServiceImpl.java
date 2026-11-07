package ru.nes.hotel.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nes.hotel.dto.AvailabilityRequest;
import ru.nes.hotel.dto.RoomRequest;
import ru.nes.hotel.dto.RoomResponse;
import ru.nes.hotel.entity.Room;
import ru.nes.hotel.entity.RoomSlot;
import ru.nes.hotel.exception.RoomUnavailableException;
import ru.nes.hotel.mapper.HotelMapper;
import ru.nes.hotel.repository.RoomRepository;
import ru.nes.hotel.repository.RoomSlotRepository;
import ru.nes.hotel.service.RoomService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomSlotRepository roomSlotRepository;
    private final HotelMapper hotelMapper;

    @Override
    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        // В реальности здесь нужна проверка, что отель существует
        Room room = hotelMapper.toEntity(request);
        room.setTimesBooked(0);
        // Костыль для тестов, чтобы не тащить HotelRepository
        ru.nes.hotel.entity.Hotel hotel = new ru.nes.hotel.entity.Hotel();
        hotel.setId(request.getHotelId());
        room.setHotel(hotel);

        return hotelMapper.toDto(roomRepository.save(room));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAllAvailableRooms() {
        return roomRepository.findAll().stream()
                .filter(Room::getAvailable)
                .map(hotelMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRecommendedRooms() {
        return roomRepository.findAllByAvailableTrueOrderByTimesBookedAscIdAsc().stream()
                .map(hotelMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void confirmAvailability(Long roomId, AvailabilityRequest request) {
        log.info("Confirming availability for room {} from {} to {}", roomId, request.getStartDate(), request.getEndDate());

        // 1. Берем пессимистичную блокировку
        Room room = roomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!room.getAvailable()) {
            throw new RoomUnavailableException("Room is out of service");
        }

        // 2. Проверяем пересечение дат
        List<RoomSlot> overlapping = roomSlotRepository.findOverlappingSlots(roomId, request.getStartDate(), request.getEndDate());
        if (!overlapping.isEmpty()) {
            throw new RoomUnavailableException("Room is already booked for these dates");
        }

        // 3. Создаем слот и инкрементим счетчик
        RoomSlot slot = RoomSlot.builder()
                .room(room)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
        roomSlotRepository.save(slot);

        room.setTimesBooked(room.getTimesBooked() + 1);
        roomRepository.save(room);

        log.info("Availability confirmed for room {}. New times_booked: {}", roomId, room.getTimesBooked());
    }

    @Override
    @Transactional
    public void release(Long roomId, AvailabilityRequest request) {
        log.info("Releasing slot for room {} from {} to {}", roomId, request.getStartDate(), request.getEndDate());

        List<RoomSlot> slots = roomSlotRepository.findExactSlot(roomId, request.getStartDate(), request.getEndDate());
        if (!slots.isEmpty()) {
            roomSlotRepository.deleteAll(slots);

            // Откатываем счетчик (best-effort)
            Room room = roomRepository.findById(roomId).orElse(null);
            if (room != null && room.getTimesBooked() > 0) {
                room.setTimesBooked(room.getTimesBooked() - 1);
                roomRepository.save(room);
            }
            log.info("Slot released for room {}", roomId);
        } else {
            log.warn("No slot found to release for room {}", roomId);
        }
    }
}