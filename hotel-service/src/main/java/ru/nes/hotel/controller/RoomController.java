package ru.nes.hotel.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.nes.hotel.dto.AvailabilityRequest;
import ru.nes.hotel.dto.RoomRequest;
import ru.nes.hotel.dto.RoomResponse;
import ru.nes.hotel.service.RoomService;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse createRoom(@Valid @RequestBody RoomRequest request) {
        return roomService.createRoom(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<RoomResponse> getAllAvailableRooms() {
        return roomService.getAllAvailableRooms();
    }

    @GetMapping("/recommend")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<RoomResponse> getRecommendedRooms() {
        return roomService.getRecommendedRooms();
    }

    // INTERNAL endpoints (не публикуются через Gateway)
    @PostMapping("/{id}/confirm-availability")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')") // Упрощенно для учебного проекта
    public void confirmAvailability(@PathVariable Long id, @Valid @RequestBody AvailabilityRequest request) {
        roomService.confirmAvailability(id, request);
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')") // Упрощенно
    public void release(@PathVariable Long id, @Valid @RequestBody AvailabilityRequest request) {
        roomService.release(id, request);
    }
}