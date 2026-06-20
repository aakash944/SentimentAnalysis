package com.example.demo.sentiment_analysis.public_controller.controller;

import com.example.demo.sentiment_analysis.config.TestSecurityConfig;
import com.example.demo.sentiment_analysis.exception.RefreshTokenException;
import com.example.demo.sentiment_analysis.jwt.model.RefreshToken;
import com.example.demo.sentiment_analysis.jwt.repository.RefreshTokenRepo;
import com.example.demo.sentiment_analysis.jwt.request.LogoutRequest;
import com.example.demo.sentiment_analysis.jwt.request.RefreshRequest;
import com.example.demo.sentiment_analysis.jwt.utili.JwtUtil;
import com.example.demo.sentiment_analysis.security.UserDetailsServiceImpl;
import com.example.demo.sentiment_analysis.user.dto.UserDto;
import com.example.demo.sentiment_analysis.user.model.Users;
import com.example.demo.sentiment_analysis.user.repository.UserRepo;
import com.example.demo.sentiment_analysis.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtUtil jwtUtil;                        // provided by TestSecurityConfig
    @Autowired
    private UserDetailsServiceImpl userDetailsService; // provided by TestSecurityConfig

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private UserRepo userRepo;
    @MockitoBean
    private RefreshTokenRepo refreshTokenRepo;

    private Users user;
    private User springUser;

    @BeforeEach
    void setUp() {
        user = new Users();
        user.setId(new ObjectId());
        user.setUserEmail("alice@test.com");
        user.setPassword("encoded");

        springUser = new User("alice@test.com", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    // -----------------------------------------------------------------------
    // POST /api/auth/sign_up
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/sign_up → 201 CREATED with new user")
    void signUp_returns201() throws Exception {
        when(userService.newUserCreate(any(UserDto.class))).thenReturn(user);

        UserDto dto = new UserDto("alice@test.com", "StrongP@ss1");
        mockMvc.perform(post("/api/auth/sign_up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userEmail").value("alice@test.com"));
    }

    @Test
    @DisplayName("POST /api/auth/sign_up → 400 BAD_REQUEST when email is blank")
    void signUp_blankEmail_returns400() throws Exception {
        UserDto invalid = new UserDto("", "StrongP@ss1");
        mockMvc.perform(post("/api/auth/sign_up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/sign_up → 400 BAD_REQUEST when email is not valid format")
    void signUp_invalidEmail_returns400() throws Exception {
        UserDto invalid = new UserDto("not-an-email", "StrongP@ss1");
        mockMvc.perform(post("/api/auth/sign_up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/sign_up → 400 when password is blank")
    void signUp_blankPassword_returns400() throws Exception {
        UserDto invalid = new UserDto("alice@test.com", "");
        mockMvc.perform(post("/api/auth/sign_up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // POST /api/auth/login
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/login → 200 OK with access and refresh tokens")
    void login_returns200WithTokens() throws Exception {
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(springUser, null, springUser.getAuthorities()));
        when(userDetailsService.loadUserByUsername("alice@test.com")).thenReturn(springUser);
        when(jwtUtil.generateToken("alice@test.com")).thenReturn("access-token-abc");
        when(jwtUtil.generateRefreshToken("alice@test.com")).thenReturn("refresh-token-xyz");
        when(userRepo.findByUserEmail("alice@test.com")).thenReturn(user);
        when(refreshTokenRepo.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        UserDto dto = new UserDto("alice@test.com", "StrongP@ss1");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-abc"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-xyz"));
    }

    @Disabled
    @Test
    @DisplayName("POST /api/auth/login → 400 BAD_REQUEST on wrong credentials")
    void login_badCredentials_returns400() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        UserDto dto = new UserDto("alice@test.com", "wrongpass");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("\"Incorrect username or password\""));
    }

    @Test
    @DisplayName("POST /api/auth/login → 400 when request body is missing required fields")
    void login_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login → refresh token entity is saved to repo")
    void login_savesRefreshTokenToRepo() throws Exception {
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(springUser, null, springUser.getAuthorities()));
        when(userDetailsService.loadUserByUsername("alice@test.com")).thenReturn(springUser);
        when(jwtUtil.generateToken(any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh-token");
        when(userRepo.findByUserEmail("alice@test.com")).thenReturn(user);
        when(refreshTokenRepo.save(any())).thenReturn(new RefreshToken());

        UserDto dto = new UserDto("alice@test.com", "StrongP@ss1");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(refreshTokenRepo).save(any(RefreshToken.class));
    }

    // -----------------------------------------------------------------------
    // POST /api/auth/refresh
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/refresh → 200 OK with new access token")
    void refresh_returns200WithNewAccessToken() throws Exception {
        RefreshToken stored = new RefreshToken();
        stored.setRefreshToken("valid-refresh");

        when(refreshTokenRepo.findByRefreshToken("valid-refresh"))
                .thenReturn(Optional.of(stored));
        when(jwtUtil.validateToken("valid-refresh")).thenReturn(true);
        when(jwtUtil.extractUserName("valid-refresh")).thenReturn("alice@test.com");
        when(jwtUtil.generateToken("alice@test.com")).thenReturn("new-access-token");

        RefreshRequest req = new RefreshRequest("valid-refresh");
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh → 400 when refresh token not found in DB")
    void refresh_tokenNotFound_returns400() throws Exception {
        when(refreshTokenRepo.findByRefreshToken("unknown-token"))
                .thenReturn(Optional.empty());

        RefreshRequest req = new RefreshRequest("unknown-token");
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/refresh → 400 when token is expired/invalid")
    void refresh_expiredToken_returns400() throws Exception {
        RefreshToken stored = new RefreshToken();
        stored.setRefreshToken("expired-token");

        when(refreshTokenRepo.findByRefreshToken("expired-token"))
                .thenReturn(Optional.of(stored));
        when(jwtUtil.validateToken("expired-token")).thenReturn(false);

        RefreshRequest req = new RefreshRequest("expired-token");
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/refresh → 400 when blank refreshToken field")
    void refresh_blankToken_returns400() throws Exception {
        RefreshRequest req = new RefreshRequest("");
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // POST /api/auth/logout
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/logout → 200 OK with success message")
    void logout_returns200WithMessage() throws Exception {
        doNothing().when(jwtUtil).logout(any(LogoutRequest.class));

        LogoutRequest req = new LogoutRequest("some-refresh-token");
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Disabled
    @Test
    @DisplayName("POST /api/auth/logout → jwtUtil.logout called with correct request")
    void logout_callsJwtUtilLogout() throws Exception {
        doNothing().when(jwtUtil).logout(any());

        LogoutRequest req = new LogoutRequest("my-token");
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(jwtUtil).logout(any(LogoutRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/logout → 400 when refreshToken is blank")
    void logout_blankToken_returns400() throws Exception {
        LogoutRequest req = new LogoutRequest("");
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // POST /api/auth/logout-all
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/logout-all → 200 OK with success message")
    void logoutAll_returns200() throws Exception {
        doNothing().when(jwtUtil).logoutAllDevices("alice@test.com");

        mockMvc.perform(post("/api/auth/logout-all")
                        .param("userEmail", "alice@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out from all devices"));
    }

    @Disabled
    @Test
    @DisplayName("POST /api/auth/logout-all → jwtUtil.logoutAllDevices called with correct email")
    void logoutAll_callsJwtUtilWithEmail() throws Exception {
        doNothing().when(jwtUtil).logoutAllDevices(anyString());

        mockMvc.perform(post("/api/auth/logout-all")
                        .param("userEmail", "alice@test.com"))
                .andExpect(status().isOk());

        verify(jwtUtil).logoutAllDevices("alice@test.com");
    }
}
