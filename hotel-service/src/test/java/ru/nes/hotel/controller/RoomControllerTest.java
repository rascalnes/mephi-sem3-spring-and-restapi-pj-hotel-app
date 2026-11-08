package ru.nes.hotel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.nes.hotel.dto.AvailabilityRequest;
import ru.nes.hotel.entity.Hotel;
import ru.nes.hotel.entity.Room;
import ru.nes.hotel.repository.HotelRepository;
import ru.nes.hotel.repository.RoomRepository;
import ru.nes.hotel.repository.RoomSlotRepository;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomSlotRepository roomSlotRepository;

    @Test
    @DisplayName("ADMIN может создать отель")
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateHotel() throws Exception {
        String json = "{\"name\":\"Test Hotel\",\"address\":\"Test Address\"}";

        mockMvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Hotel"));
    }

    @Test
    @DisplayName("USER получает 403 при попытке создать отель")
    @WithMockUser(roles = "USER")
    void userCannotCreateHotel() throws Exception {
        String json = "{\"name\":\"Test Hotel\",\"address\":\"Test Address\"}";

        mockMvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Конфликт 409 при попытке занять занятые даты")
    @WithMockUser(roles = "ADMIN")
    void shouldReturnConflictForOverlappingDates() throws Exception {
        // Arrange
        Hotel hotel = Hotel.builder().name("H1").address("A1").build();
        hotelRepository.save(hotel);
        Room room = Room.builder().hotel(hotel).number("101").available(true).timesBooked(0).build();
        roomRepository.save(room);

        AvailabilityRequest req = new AvailabilityRequest();
        req.setStartDate(LocalDate.now().plusDays(1));
        req.setEndDate(LocalDate.now().plusDays(5));

        // Первое бронирование (успех)
        mockMvc.perform(post("/api/rooms/" + room.getId() + "/confirm-availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Второе бронирование на те же даты (ошибка 409)
        mockMvc.perform(post("/api/rooms/" + room.getId() + "/confirm-availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }
}