package ru.nes.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nes.booking.entity.ProcessedRequest;

public interface ProcessedRequestRepository extends JpaRepository<ProcessedRequest, String> {
}