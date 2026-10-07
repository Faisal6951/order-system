package com.orderplatform.order_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationErrors(
                        MethodArgumentNotValidException ex) {

                Map<String, String> fieldErrors = new HashMap<>();
                ex.getBindingResult().getFieldErrors()
                                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Validation failed");
                error.setFieldErrors(fieldErrors);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(OrderNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.NOT_FOUND.value(),
                                ex.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        @ExceptionHandler(InsufficientStockException.class)
        public ResponseEntity<ErrorResponse> handleInsufficientStockException(InsufficientStockException ex) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(InventoryNotFoundException.class)
        public ResponseEntity<ErrorResponse> InventoryNotFoundException(InventoryNotFoundException ex) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                ex.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        @ExceptionHandler(ProductNotFoundException.class)
        public ResponseEntity<ErrorResponse> ProductNotFoundException(ProductNotFoundException ex) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.NOT_FOUND.value(),
                                ex.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        @ExceptionHandler(InvalidRefreshTokenException.class)
        public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.UNAUTHORIZED.value(),
                                ex.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        @ExceptionHandler(UserAlreadyExistsException.class)
        public ResponseEntity<ErrorResponse> UserAlreadyExistsException(UserAlreadyExistsException ex) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.CONFLICT.value(),
                                ex.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        @ExceptionHandler(InvalidStatusTransitionException.class)
        public ResponseEntity<ErrorResponse> InvalidStatusTransitionException(InvalidStatusTransitionException ex) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.CONFLICT.value(),
                                ex.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleTypeMismatch(IllegalArgumentException ex) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Invalid status value. Accepted values: PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleDuplicateEntry(DataIntegrityViolationException ex) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.CONFLICT.value(),
                                "Resource already exists");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
}