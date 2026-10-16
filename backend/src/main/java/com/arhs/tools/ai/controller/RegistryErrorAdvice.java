package com.arhs.tools.ai.controller;

import com.arhs.tools.ai.exception.RegistryConflictException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice(assignableTypes = {DuplicateCaseController.class, PersonMergeController.class, CivilRegistryController.class})
public class RegistryErrorAdvice {
    @ExceptionHandler(RegistryConflictException.class)
    public ResponseEntity<?> conflict(RegistryConflictException e) {
        return ResponseEntity.status(409).body(Map.of("code", "CONFLICT", "message", e.getMessage(), "resource", e.getResource()));
    }
    @ExceptionHandler({ConcurrencyFailureException.class, OptimisticLockException.class, DataIntegrityViolationException.class})
    public ResponseEntity<?> race(Exception e) {
        return ResponseEntity.status(409).body(Map.of("code", "CONCURRENT_CHANGE", "message", "A record changed or an identifier is no longer available. Refresh and review again."));
    }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<?> invalid(Exception e) {
        // Validation messages come from our allowlisted checks; parser details never escape.
        String message = e instanceof IllegalArgumentException && !(e instanceof MethodArgumentTypeMismatchException)
                ? e.getMessage() : "Request is invalid";
        return ResponseEntity.badRequest().body(Map.of("code", "INVALID_REQUEST", "message", message == null ? "Request is invalid" : message));
    }
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> missing(Exception e) {
        return ResponseEntity.status(404).body(Map.of("code", "NOT_FOUND", "message", "Requested record is unavailable"));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse requestError) {
            return ResponseEntity.status(requestError.getStatusCode()).body(Map.of("code", "REQUEST_ERROR", "message", "Request could not be processed"));
        }
        return ResponseEntity.internalServerError().body(Map.of("code", "SERVER_ERROR", "message", "The operation could not be completed"));
    }
}
