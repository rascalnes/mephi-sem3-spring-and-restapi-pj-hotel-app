package ru.nes.hotel.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.nes.hotel.entity.Room;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // Сортировка для рекомендаций
    List<Room> findAllByAvailableTrueOrderByTimesBookedAscIdAsc();

    // Пессимистичная блокировка для предотвращения гонок
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdWithLock(@Param("id") Long id);
}