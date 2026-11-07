package ru.nes.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nes.hotel.entity.Hotel;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
}