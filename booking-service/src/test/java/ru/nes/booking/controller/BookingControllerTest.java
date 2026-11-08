package ru.nes.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.nes.booking.client.HotelClient;
import ru.nes.booking.dto.AvailabilityRequest;
import ru.nes.booking.dto.BookingRequest;
import ru.nes.booking.entity.Booking;
import ru.nes.booking.entity.BookingStatus;
import ru.nes.booking.repository.BookingRepository;
import ru.nes.booking.repository.ProcessedRequestRepository;
import ru.nes.common.security.JwtService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ProcessedRequestRepository processedRequestRepository;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private HotelClient hotelClient;

    private BookingRequest validRequest;
    private String userToken;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        processedRequestRepository.deleteAll();

        validRequest = new BookingRequest();
        validRequest.setAutoSelect(true);
        validRequest.setStartDate(LocalDate.now().plusDays(1));
        validRequest.setEndDate(LocalDate.now().plusDays(5));

        // Генерация реального JWT для теста
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        claims.put("userId", 1L);
        userToken = jwtService.generateToken("user", claims);
    }

    @Nested
    @DisplayName("Создание бронирования (POST /booking)")
    class CreateBookingTests {

        @Test
        @DisplayName("Успешная Сага: PENDING -> CONFIRMED")
        void shouldCreateBookingSuccessfully() throws Exception {
            // Arrange
            when(hotelClient.getRecommendedRoomId(anyString())).thenReturn(101L);
            when(hotelClient.confirmAvailability(anyLong(), any(AvailabilityRequest.class), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // Act
            mockMvc.perform(post("/booking")
                            .header("Authorization", "Bearer " + userToken)
                            .header("Idempotency-Key", "req-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    // Assert
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.roomId").value(101));

            assertEquals(1, bookingRepository.count());
            assertEquals(BookingStatus.CONFIRMED, bookingRepository.findAll().get(0).getStatus());
        }

        @Test
        @DisplayName("Ошибка Hotel Service: PENDING -> CANCELLED (Компенсация)")
        void shouldCancelBookingWhenHotelFails() throws Exception {
            // Arrange
            when(hotelClient.getRecommendedRoomId(anyString())).thenReturn(101L);
            when(hotelClient.confirmAvailability(anyLong(), any(AvailabilityRequest.class), anyString()))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Hotel is down")));

            // Act
            mockMvc.perform(post("/booking")
                            .header("Authorization", "Bearer " + userToken)
                            .header("Idempotency-Key", "req-124")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    // Assert
                    .andExpect(status().isInternalServerError());

            assertEquals(1, bookingRepository.count());
            assertEquals(BookingStatus.CANCELLED, bookingRepository.findAll().get(0).getStatus());
            verify(hotelClient, times(1)).release(anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("Идемпотентность: повторный запрос с тем же Idempotency-Key")
        void shouldReturnSameResponseForDuplicateRequest() throws Exception {
            // Arrange
            when(hotelClient.getRecommendedRoomId(anyString())).thenReturn(101L);
            when(hotelClient.confirmAvailability(anyLong(), any(AvailabilityRequest.class), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // Act 1
            mockMvc.perform(post("/booking")
                            .header("Authorization", "Bearer " + userToken)
                            .header("Idempotency-Key", "req-125")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated());

            // Act 2 (Повтор)
            mockMvc.perform(post("/booking")
                            .header("Authorization", "Bearer " + userToken)
                            .header("Idempotency-Key", "req-125")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    // Assert
                    .andExpect(status().isCreated());

            // Проверка, что в БД создалась только 1 бронь, и Hotel Service вызвался 1 раз
            assertEquals(1, bookingRepository.count());
            verify(hotelClient, times(1)).confirmAvailability(anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("Ошибка 400: Невалидные даты")
        void shouldReturnBadRequestForInvalidDates() throws Exception {
            // Arrange
            validRequest.setStartDate(LocalDate.now().plusDays(5));
            validRequest.setEndDate(LocalDate.now().plusDays(1)); // Конец раньше начала, но обе в будущем

            // Act
            mockMvc.perform(post("/booking")
                            .header("Authorization", "Bearer " + userToken)
                            .header("Idempotency-Key", "req-126")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    // Assert
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("startDate must be before endDate"));
        }
    }

    @Nested
    @DisplayName("Получение броней (GET /bookings)")
    class GetBookingsTests {

        @Test
        @DisplayName("Пользователь видит только свои брони")
        void shouldReturnOnlyUserBookings() throws Exception {
            // Arrange
            Booking booking = Booking.builder()
                    .userId(1L) // ID совпадает с тем, что в JWT
                    .roomId(101L)
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .status(BookingStatus.CONFIRMED)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            bookingRepository.save(booking);

            // Act
            mockMvc.perform(get("/bookings?page=0&size=5")
                            .header("Authorization", "Bearer " + userToken))
                    // Assert
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }
    }
}