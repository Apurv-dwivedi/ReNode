package com.renode.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleExceptions(Exception ex) {
        Map<String, String> errorResponse = new HashMap<>();
        // Clean JSON format me error message bhejenge
        errorResponse.put("error", ex.getMessage() != null ? ex.getMessage() : "Internal Server Error occurred");

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}