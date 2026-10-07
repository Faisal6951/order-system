package com.orderplatform.order_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderplatform.order_service.dto.AuthResponse;
import com.orderplatform.order_service.dto.LoginRequest;
import com.orderplatform.order_service.dto.RefreshRequest;
import com.orderplatform.order_service.dto.RegisterRequest;
import com.orderplatform.order_service.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequestMapping("/api/auth")
@RequiredArgsConstructor
@RestController
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@RequestBody @Valid RegisterRequest request) {

        AuthResponse register = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(register);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@RequestBody @Valid LoginRequest request) {
        AuthResponse login = authService.login(request);
        return ResponseEntity.ok(login);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
