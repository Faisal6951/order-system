package com.orderplatform.order_service.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.orderplatform.order_service.dto.AuthResponse;
import com.orderplatform.order_service.dto.LoginRequest;
import com.orderplatform.order_service.dto.RefreshRequest;
import com.orderplatform.order_service.dto.RegisterRequest;
import com.orderplatform.order_service.exception.InvalidRefreshTokenException;
import com.orderplatform.order_service.exception.UserAlreadyExistsException;
import com.orderplatform.order_service.model.User;
import com.orderplatform.order_service.repository.UserRepository;
import com.orderplatform.order_service.security.JwtUtil;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException();
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getUserRole().name());
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setRefreshToken(refreshTokenService.createRefreshToken(user.getEmail()));
        return response;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getUserRole().name());
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setRefreshToken(refreshTokenService.createRefreshToken(user.getEmail()));
        return response;
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {

        String email = refreshTokenService.verifyAndConsume(request.getRefreshToken());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidRefreshTokenException());

        String token = jwtUtil.generateToken(user.getEmail(), user.getUserRole().name());
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setRefreshToken(refreshTokenService.createRefreshToken(user.getEmail()));
        return response;

    }

    public void logout(RefreshRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
    }
}