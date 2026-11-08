package ru.nes.booking.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.nes.booking.dto.AvailabilityRequest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class HotelClient {

    private final RestClient restClient;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor(); // Java 21

    public HotelClient(RestClient.Builder loadBalancedRestClientBuilder) {
        this.restClient = loadBalancedRestClientBuilder
                .baseUrl("http://HOTEL-SERVICE")
                .build();
    }

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

    // Паттерны отказоустойчивости
    @TimeLimiter(name = "hotelService")
    @CircuitBreaker(name = "hotelService", fallbackMethod = "fallbackConfirm")
    @Retry(name = "hotelService", fallbackMethod = "fallbackConfirm")
    public CompletableFuture<Void> confirmAvailability(Long roomId, AvailabilityRequest request, String token) {
        return CompletableFuture.runAsync(() -> {
            log.info("Calling Hotel Service to confirm room {} from {} to {}", roomId, request.getStartDate(), request.getEndDate());
            restClient.post()
                    .uri("/api/rooms/{id}/confirm-availability", roomId)
                    .header("Authorization", token)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        }, executor);
    }

    // Fallback вызывается, если исчерпаны Retry или сработал Circuit Breaker
    public CompletableFuture<Void> fallbackConfirm(Long roomId, AvailabilityRequest request, String token, Throwable t) {
        log.error("Fallback triggered for room {}: {}", roomId, t.getMessage());
        return CompletableFuture.failedFuture(t);
    }

    // Компенсация (Best-effort)
    @Retry(name = "hotelService") // Попытка отправить release до 3 раз
    public void release(Long roomId, AvailabilityRequest request, String token) {
        log.info("Compensating: calling Hotel Service to release room {} from {} to {}", roomId, request.getStartDate(), request.getEndDate());
        restClient.post()
                .uri("/api/rooms/{id}/release", roomId)
                .header("Authorization", token)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}