package com.example.demo.sentiment_analysis.admin_controller;

import com.example.demo.sentiment_analysis.config.TestSecurityConfig;
import com.example.demo.sentiment_analysis.slice_response_dto.PaginatedResponse;
import com.example.demo.sentiment_analysis.user.service.UserService;
import com.example.demo.sentiment_analysis.user.user_response.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminInfo.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminInfoTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserService userService;

    @Test
    @DisplayName("GET /Admin_userInfo/getAllUser → 200 OK with users list and success message")
    void getAllUser_returns200WithUsers() throws Exception {
        PaginatedResponse<UserResponse> paged = new PaginatedResponse<>();
        paged.setContent(List.of(
                new UserResponse("alice@test.com", LocalDateTime.now()),
                new UserResponse("bob@test.com", LocalDateTime.now())));
        paged.setPageNumber(0);
        paged.setPageSize(10);
        paged.setFirst(true);
        paged.setLast(true);
        paged.setHasNext(false);

        when(userService.getUserDb(any(Pageable.class))).thenReturn(paged);

        mockMvc.perform(get("/Admin_userInfo/getAllUser").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Users fetched successfully"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].userEmail").value("alice@test.com"));
    }

    @Test
    @DisplayName("GET /Admin_userInfo/getAllUser → 200 OK with empty list")
    void getAllUser_emptyList_returns200() throws Exception {
        PaginatedResponse<UserResponse> paged = new PaginatedResponse<>();
        paged.setContent(List.of());
        when(userService.getUserDb(any(Pageable.class))).thenReturn(paged);

        mockMvc.perform(get("/Admin_userInfo/getAllUser").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @DisplayName("GET /Admin_userInfo/getAllUser → error field is null")
    void getAllUser_errorFieldIsNull() throws Exception {
        PaginatedResponse<UserResponse> paged = new PaginatedResponse<>();
        paged.setContent(List.of());
        when(userService.getUserDb(any(Pageable.class))).thenReturn(paged);

        mockMvc.perform(get("/Admin_userInfo/getAllUser").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    @DisplayName("GET /Admin_userInfo/getAllUser → pagination metadata returned correctly")
    void getAllUser_paginationMetadata() throws Exception {
        PaginatedResponse<UserResponse> paged = new PaginatedResponse<>();
        paged.setContent(List.of(new UserResponse("alice@test.com", LocalDateTime.now())));
        paged.setPageNumber(0);
        paged.setPageSize(5);
        paged.setFirst(true);
        paged.setLast(true);
        paged.setHasNext(false);
        when(userService.getUserDb(any(Pageable.class))).thenReturn(paged);

        mockMvc.perform(get("/Admin_userInfo/getAllUser").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNumber").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(5))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("GET /Admin_userInfo/getAllUser → userService.getUserDb called with pageable")
    void getAllUser_callsServiceWithPageable() throws Exception {
        PaginatedResponse<UserResponse> paged = new PaginatedResponse<>();
        paged.setContent(List.of());
        when(userService.getUserDb(any(Pageable.class))).thenReturn(paged);

        mockMvc.perform(get("/Admin_userInfo/getAllUser")
                        .param("page", "0").param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(userService).getUserDb(any(Pageable.class));
    }
}
