package ru.nes.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.nes.hotel.entity.RoomSlot;

import java.time.LocalDate;
import java.util.List;

public interface RoomSlotRepository extends JpaRepository<RoomSlot, Long> {

    // Проверка на пересечение дат
    @Query("select s from RoomSlot s where s.room.id = :roomId and s.startDate < :endDate and s.endDate > :startDate")
    List<RoomSlot> findOverlappingSlots(@Param("roomId") Long roomId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    // Поиск конкретного слота для удаления (компенсации)
    @Query("select s from RoomSlot s where s.room.id = :roomId and s.startDate = :startDate and s.endDate = :endDate")
    List<RoomSlot> findExactSlot(@Param("roomId") Long roomId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);
}