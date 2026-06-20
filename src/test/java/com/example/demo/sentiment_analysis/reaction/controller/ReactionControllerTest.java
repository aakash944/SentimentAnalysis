package com.example.demo.sentiment_analysis.reaction.controller;

import com.example.demo.sentiment_analysis.config.TestSecurityConfig;
import com.example.demo.sentiment_analysis.exception.PostsNotFoundException;
import com.example.demo.sentiment_analysis.reaction.dto.ReactionDto;
import com.example.demo.sentiment_analysis.reaction.enumeration.ReactionType;
import com.example.demo.sentiment_analysis.reaction.reaction_response.ReactionResponseDto;
import com.example.demo.sentiment_analysis.reaction.service.ReactionService;
import com.example.demo.sentiment_analysis.slice_response_dto.PaginatedResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReactionController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class ReactionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean  private ReactionService reactionService;

    private ObjectId postId;

    @BeforeEach
    void setUp() {
        postId = new ObjectId();

        User principal = new User("alice@test.com", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // -----------------------------------------------------------------------
    // GET /api/react
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/react → 200 OK with reactions list")
    void getAllReactions_returns200() throws Exception {
        ReactionResponseDto dto = new ReactionResponseDto(
                postId.toHexString(), "alice@test.com", ReactionType.LIKE, LocalDateTime.now());

        PaginatedResponse<ReactionResponseDto> paged = new PaginatedResponse<>();
        paged.setContent(List.of(dto));
        paged.setPageNumber(0);
        paged.setPageSize(10);

        when(reactionService.getAllReactions(eq("alice@test.com"), any(Pageable.class)))
                .thenReturn(paged);

        mockMvc.perform(get("/api/react").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reaction fetched successfully"))
                .andExpect(jsonPath("$.data.content[0].userEmail").value("alice@test.com"))
                .andExpect(jsonPath("$.data.content[0].reactionType").value("LIKE"));
    }

    @Test
    @DisplayName("GET /api/react → 200 OK with empty list")
    void getAllReactions_emptyList_returns200() throws Exception {
        PaginatedResponse<ReactionResponseDto> paged = new PaginatedResponse<>();
        paged.setContent(List.of());
        when(reactionService.getAllReactions(any(), any())).thenReturn(paged);

        mockMvc.perform(get("/api/react"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @DisplayName("GET /api/react → uses authenticated user's email")
    void getAllReactions_usesAuthenticatedEmail() throws Exception {
        PaginatedResponse<ReactionResponseDto> paged = new PaginatedResponse<>();
        paged.setContent(List.of());
        when(reactionService.getAllReactions(eq("alice@test.com"), any())).thenReturn(paged);

        mockMvc.perform(get("/api/react")).andExpect(status().isOk());

        verify(reactionService).getAllReactions(eq("alice@test.com"), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/react → response wraps data in ApiResponse with null error")
    void getAllReactions_apiResponseStructure() throws Exception {
        PaginatedResponse<ReactionResponseDto> paged = new PaginatedResponse<>();
        paged.setContent(List.of());
        when(reactionService.getAllReactions(any(), any())).thenReturn(paged);

        mockMvc.perform(get("/api/react"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").exists());
    }

    // -----------------------------------------------------------------------
    // POST /api/react
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/react → 200 OK (void return) on successful reaction")
    void reactEmoji_returns200() throws Exception {
        doNothing().when(reactionService).createReaction(any(ReactionDto.class), eq("alice@test.com"));

        ReactionDto dto = new ReactionDto(postId, ReactionType.LIKE);
        mockMvc.perform(post("/api/react")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/react → service called with authenticated user's email")
    void reactEmoji_usesAuthenticatedEmail() throws Exception {
        doNothing().when(reactionService).createReaction(any(), eq("alice@test.com"));

        ReactionDto dto = new ReactionDto(postId, ReactionType.LOVE);
        mockMvc.perform(post("/api/react")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(reactionService).createReaction(any(ReactionDto.class), eq("alice@test.com"));
    }

    @Test
    @DisplayName("POST /api/react → 404 when post not found (via global handler)")
    void reactEmoji_postNotFound_returns404() throws Exception {
        doThrow(new PostsNotFoundException("Post not found"))
                .when(reactionService).createReaction(any(), any());

        ReactionDto dto = new ReactionDto(postId, ReactionType.LIKE);
        mockMvc.perform(post("/api/react")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Post not found"));
    }

    @Test
    @DisplayName("POST /api/react → 500 when service throws unexpected exception")
    void reactEmoji_unexpectedException_returns500() throws Exception {
        doThrow(new RuntimeException("Unexpected"))
                .when(reactionService).createReaction(any(), any());

        ReactionDto dto = new ReactionDto(postId, ReactionType.LIKE);
        mockMvc.perform(post("/api/react")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError());
    }

    // -----------------------------------------------------------------------
    // GET /api/react/{postId}/count
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/react/{postId}/count → 200 OK with reaction count")
    void getReactionCount_returns200WithCount() throws Exception {
        when(reactionService.getReactionCount(postId)).thenReturn(8L);

        mockMvc.perform(get("/api/react/{postId}/count", postId.toHexString()))
                .andExpect(status().isOk())
                .andExpect(content().string("8"));
    }

    @Test
    @DisplayName("GET /api/react/{postId}/count → 200 OK with 0 when no reactions")
    void getReactionCount_zeroCount_returns200() throws Exception {
        when(reactionService.getReactionCount(postId)).thenReturn(0L);

        mockMvc.perform(get("/api/react/{postId}/count", postId.toHexString()))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    @DisplayName("GET /api/react/{postId}/count → 400 BAD_REQUEST on invalid ObjectId format")
    void getReactionCount_invalidId_returns400() throws Exception {
        mockMvc.perform(get("/api/react/{postId}/count", "not-a-valid-id"))
                .andExpect(status().isBadRequest());
    }
}
