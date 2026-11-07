package ru.nes.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nes.booking.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}