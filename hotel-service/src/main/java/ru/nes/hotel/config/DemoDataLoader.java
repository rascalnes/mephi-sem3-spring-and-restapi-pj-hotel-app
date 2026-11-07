package ru.nes.hotel.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.nes.hotel.entity.Hotel;
import ru.nes.hotel.entity.Room;
import ru.nes.hotel.repository.HotelRepository;
import ru.nes.hotel.repository.RoomRepository;

@Component
@RequiredArgsConstructor
@Profile("demo")
public class DemoDataLoader implements CommandLineRunner {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @Override
    public void run(String... args) {
        if (hotelRepository.count() == 0) {
            Hotel hotel1 = Hotel.builder().name("Grand Hotel").address("123 Main St").build();
            Hotel hotel2 = Hotel.builder().name("Budget Inn").address("456 Elm St").build();
            hotelRepository.save(hotel1);
            hotelRepository.save(hotel2);

            roomRepository.save(Room.builder().hotel(hotel1).number("101").available(true).timesBooked(0).build());
            roomRepository.save(Room.builder().hotel(hotel1).number("102").available(true).timesBooked(0).build());
            roomRepository.save(Room.builder().hotel(hotel2).number("201").available(true).timesBooked(0).build());
        }
    }
}