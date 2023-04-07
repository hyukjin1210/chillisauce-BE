package com.example.chillisauce.reservations.exception;

import com.example.chillisauce.message.ResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ReservationExceptionHandler {
    @ExceptionHandler(value = { ReservationException.class })
    protected ResponseEntity<ResponseMessage> handleReservationException(ReservationException e) {
        log.error("handleReservationException throw ReservationException : {}", e.getErrorCode());
        return ResponseMessage.responseError(e.getErrorCode());
    }
}
