package ru.nes.booking.service;

import ru.nes.booking.dto.AuthRequest;
import ru.nes.booking.dto.AuthResponse;
import ru.nes.booking.dto.UserRequest;
import ru.nes.booking.entity.User;

import java.util.List;

public interface UserService {
    AuthResponse register(AuthRequest request);
    AuthResponse authenticate(AuthRequest request);
    List<User> getAllUsers();
    User createUser(UserRequest request);
    void deleteUser(Long id);
}