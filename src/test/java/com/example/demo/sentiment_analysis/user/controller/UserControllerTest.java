package com.example.demo.sentiment_analysis.user.controller;

import com.example.demo.sentiment_analysis.config.TestSecurityConfig;
import com.example.demo.sentiment_analysis.user.dto.UserDto;
import com.example.demo.sentiment_analysis.user.model.Users;
import com.example.demo.sentiment_analysis.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean  private UserService userService;

    private ObjectId userId;
    private Users updatedUser;

    @BeforeEach
    void setUp() {
        userId = new ObjectId();

        // Mock a security principal in the context
        User principal = new User("alice@test.com", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        updatedUser = new Users();
        updatedUser.setId(userId);
        updatedUser.setUserEmail("alice@test.com");
    }

    // -----------------------------------------------------------------------
    // DELETE /api/user/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/user/{id} → 204 NO_CONTENT on success")
    void deleteUser_returns204() throws Exception {
        doNothing().when(userService).removeUser(eq(userId), eq("alice@test.com"));

        mockMvc.perform(delete("/api/user/{id}", userId.toHexString()))
                .andExpect(status().isNoContent());

        verify(userService).removeUser(eq(userId), eq("alice@test.com"));
    }

    @Test
    @DisplayName("DELETE /api/user/{id} → service called with correct userId and email")
    void deleteUser_passesCorrectArguments() throws Exception {
        doNothing().when(userService).removeUser(any(ObjectId.class), any());

        mockMvc.perform(delete("/api/user/{id}", userId.toHexString()))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).removeUser(eq(userId), eq("alice@test.com"));
    }

    @Test
    @DisplayName("DELETE /api/user/{id} → 500 when service throws RuntimeException")
    void deleteUser_serviceThrows_returns500() throws Exception {
        doThrow(new RuntimeException("Unexpected")).when(userService)
                .removeUser(any(ObjectId.class), any());

        mockMvc.perform(delete("/api/user/{id}", userId.toHexString()))
                .andExpect(status().isInternalServerError());
    }

    // -----------------------------------------------------------------------
    // PUT /api/user/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/user/{id} → 200 OK with updated user body")
    void updateUser_returns200WithUpdatedUser() throws Exception {
        UserDto dto = new UserDto("alice@test.com", "NewPass1!");
        when(userService.newUserUpdate(eq(userId), any(UserDto.class), eq("alice@test.com")))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/user/{id}", userId.toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value("alice@test.com"));
    }

    @Test
    @DisplayName("PUT /api/user/{id} → 400 BAD_REQUEST when request body is invalid (blank email)")
    void updateUser_invalidBody_returns400() throws Exception {
        UserDto invalid = new UserDto("", "pw"); // blank email violates @NotBlank @Email

        mockMvc.perform(put("/api/user/{id}", userId.toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/user/{id} → 500 when service throws exception")
    void updateUser_serviceThrows_returns500() throws Exception {
        UserDto dto = new UserDto("alice@test.com", "NewPass1!");
        when(userService.newUserUpdate(any(), any(), any()))
                .thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(put("/api/user/{id}", userId.toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("PUT /api/user/{id} → service called with authenticated user's email")
    void updateUser_usesAuthenticatedEmail() throws Exception {
        UserDto dto = new UserDto("alice@test.com", "NewPass1!");
        when(userService.newUserUpdate(any(), any(), eq("alice@test.com")))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/user/{id}", userId.toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(userService).newUserUpdate(eq(userId), any(UserDto.class), eq("alice@test.com"));
    }
}
