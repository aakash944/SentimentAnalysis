package com.example.demo.sentiment_analysis.public_controller.service;

import com.example.demo.sentiment_analysis.jwt.request.RefreshRequest;
import com.example.demo.sentiment_analysis.jwt.response.AuthResponse;
import com.example.demo.sentiment_analysis.public_controller.login.LoginRequest;
import com.example.demo.sentiment_analysis.jwt.request.LogoutRequest;
import com.example.demo.sentiment_analysis.jwt.utili.JwtUtil;
import com.example.demo.sentiment_analysis.jwt.model.RefreshToken;
import com.example.demo.sentiment_analysis.jwt.repository.RefreshTokenRepo;
import com.example.demo.sentiment_analysis.user.model.Users;
import com.example.demo.sentiment_analysis.user.repository.UserRepo;
import org.bson.types.ObjectId;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
@Service
public class AuthService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepo refreshTokenRepo;

    public AuthService(UserRepo userRepo, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, RefreshTokenRepo refreshTokenRepo) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepo = refreshTokenRepo;
    }

    public AuthResponse login(LoginRequest request) {
        Users user = userRepo.findByUserEmail(request.getUserEmail());

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken = jwtUtil.generateToken(user.getUserEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserEmail());

        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setId(new ObjectId());
        tokenEntity.setUserId(user.getId());
        tokenEntity.setUserEmail(user.getUserEmail());
        tokenEntity.setRefreshToken(refreshToken);
        tokenEntity.setCreatedAt(new Date());
        tokenEntity.setExpiresAt(new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000));

        refreshTokenRepo.save(tokenEntity);

        return new AuthResponse(accessToken, refreshToken, "Bearer");
    }

    public AuthResponse refresh(RefreshRequest request) {
        String incomingRefreshToken = request.getRefreshToken();

        if (incomingRefreshToken == null || incomingRefreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token is required");
        }

        String userEmail = jwtUtil.extractUserName(incomingRefreshToken);
        if (userEmail == null) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        RefreshToken stored = refreshTokenRepo.findByRefreshToken(incomingRefreshToken)
                .orElseThrow(() -> new BadCredentialsException("Refresh token not found"));

        if (!jwtUtil.validateToken(incomingRefreshToken)) {
            refreshTokenRepo.delete(stored);
            throw new BadCredentialsException("Refresh token expired");
        }

        String newAccessToken = jwtUtil.generateToken(userEmail);

        return new AuthResponse(newAccessToken, incomingRefreshToken, "Bearer");
    }

    public void logout(LogoutRequest request) {
        String refreshToken = request.getRefreshToken();

        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepo.deleteByRefreshToken(refreshToken);
        }
    }

    public void logoutAllDevices(String userEmail) {
        refreshTokenRepo.deleteByUserEmail(userEmail);
    }
}

