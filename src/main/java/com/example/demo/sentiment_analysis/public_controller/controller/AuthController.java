package com.example.demo.sentiment_analysis.public_controller.controller;

import com.example.demo.sentiment_analysis.jwt.request.RefreshRequest;
import com.example.demo.sentiment_analysis.jwt.request.LogoutRequest;
import com.example.demo.sentiment_analysis.user.dto.UserDto;
import com.example.demo.sentiment_analysis.exception.RefreshTokenException;
import com.example.demo.sentiment_analysis.public_controller.service.AuthService;
import com.example.demo.sentiment_analysis.jwt.model.RefreshToken;
import com.example.demo.sentiment_analysis.jwt.repository.RefreshTokenRepo;
import com.example.demo.sentiment_analysis.security.UserDetailsServiceImpl;
import com.example.demo.sentiment_analysis.user.model.Users;
import com.example.demo.sentiment_analysis.user.repository.UserRepo;
import com.example.demo.sentiment_analysis.user.service.UserService;
import com.example.demo.sentiment_analysis.jwt.utili.JwtUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;
    private final AuthService authService;
    private final UserRepo userRepo;
    private final RefreshTokenRepo refreshTokenRepo;

    public AuthController(UserService userService, AuthenticationManager authenticationManager, UserDetailsServiceImpl userDetailsService, JwtUtil jwtUtil, AuthService authService, UserRepo userRepo, RefreshTokenRepo refreshTokenRepo) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.authService = authService;
        this.userRepo = userRepo;
        this.refreshTokenRepo = refreshTokenRepo;
    }

    @PostMapping("/sign_up")
    public ResponseEntity<Users> createUser(@Valid @RequestBody UserDto userDto) {
        Users users = userService.newUserCreate(userDto);
        return new ResponseEntity<>(users, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody UserDto userDto) {
        try {
            authenticationManager.authenticate
                    (new UsernamePasswordAuthenticationToken(userDto.getUserEmail(),
                            userDto.getPassword()));
            UserDetails userDetails = userDetailsService.loadUserByUsername(userDto.getUserEmail());
            String accessToken = jwtUtil.generateToken(userDetails.getUsername());

            String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername());

            Users user = userRepo.findByUserEmail(userDto.getUserEmail());

            RefreshToken refreshTokenEntity = new RefreshToken();
            refreshTokenEntity.setUserId(user.getId());

            refreshTokenEntity.setUserEmail(user.getUserEmail());

            refreshTokenEntity.setRefreshToken(refreshToken);

            refreshTokenEntity.setCreatedAt(new Date());

            refreshTokenEntity.setExpiresAt
                    (new Date(System.currentTimeMillis() + refreshExpirationMs));
            refreshTokenRepo.save(refreshTokenEntity);

            Map<String, String> response = new HashMap<>();
            response.put("accessToken", accessToken);

            response.put("refreshToken", refreshToken);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {

            log.error("Exception occurred while login", e);

            return new ResponseEntity<>("Incorrect username or password", HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(
            @Valid @RequestBody RefreshRequest request) {

        String refreshToken = request.getRefreshToken();

//        RefreshToken storedToken =
        refreshTokenRepo.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RefreshTokenException("Refresh token not found"));

        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RefreshTokenException("Refresh token expired");
        }
        String username = jwtUtil.extractUserName(refreshToken);
        String newAccessToken = jwtUtil.generateToken(username);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAllDevices(@RequestParam String userEmail) {
        authService.logoutAllDevices(userEmail);
        return ResponseEntity.ok(Map.of("message", "Logged out from all devices"));
    }
}
