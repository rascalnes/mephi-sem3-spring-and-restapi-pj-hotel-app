package ru.nes.hotel.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.nes.common.dto.ErrorDTO;

import java.time.LocalDateTime;

@RestControllerAdvice
public class HotelExceptionHandler {

    @ExceptionHandler(RoomUnavailableException.class)
    public ResponseEntity<ErrorDTO> handleRoomUnavailable(RoomUnavailableException ex) {
        ErrorDTO error = ErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}