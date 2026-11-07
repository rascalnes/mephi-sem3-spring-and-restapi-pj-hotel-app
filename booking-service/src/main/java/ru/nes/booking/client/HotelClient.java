package ru.nes.booking.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.nes.booking.dto.AvailabilityRequest;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class HotelClient {

    private final RestClient restClient;

    public HotelClient(RestClient.Builder loadBalancedRestClientBuilder) {
        this.restClient = loadBalancedRestClientBuilder
                .baseUrl("http://HOTEL-SERVICE")
                .build();
    }

    // Получаем рекомендованный номер (первый в списке)
    @SuppressWarnings("unchecked")
    public Long getRecommendedRoomId(String token) {
        List<Map<String, Object>> rooms = restClient.get()
                .uri("/api/rooms/recommend")
                .header("Authorization", token)
                .retrieve()
                .body(List.class);

        if (rooms == null || rooms.isEmpty()) {
            throw new RuntimeException("No available rooms found");
        }
        return ((Number) rooms.get(0).get("id")).longValue();
    }

    // Подтверждаем доступность (в следующем этапе обернем в Resilience4j)
    public void confirmAvailability(Long roomId, AvailabilityRequest request, String token) {
        restClient.post()
                .uri("/api/rooms/{id}/confirm-availability", roomId)
                .header("Authorization", token)
                .body(request)
                .retrieve()
                .toBodilessEntity(); // Если вернется 409, RestClient выбросит исключение
    }
}