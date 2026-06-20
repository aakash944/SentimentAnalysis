package com.example.demo.sentiment_analysis.comment.controller;

import com.example.demo.sentiment_analysis.config.TestSecurityConfig;
import com.example.demo.sentiment_analysis.ai.enumeration.SentimentType;
import com.example.demo.sentiment_analysis.comment.comment_request.CommentRequest;
import com.example.demo.sentiment_analysis.comment.comment_response.CommentResponseDto;
import com.example.demo.sentiment_analysis.comment.model.Comment;
import com.example.demo.sentiment_analysis.comment.service.CommentService;
import com.example.demo.sentiment_analysis.exception.CommentNotFoundException;
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

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CommentController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean  private CommentService commentService;

    private ObjectId postId;
    private ObjectId commentId;

    @BeforeEach
    void setUp() {
        postId    = new ObjectId();
        commentId = new ObjectId();

        User principal = new User("alice@test.com", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // -----------------------------------------------------------------------
    // GET /api/comment
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/comment → 200 OK with comment list and message")
    void getAllComments_returns200() throws Exception {
        CommentResponseDto dto = new CommentResponseDto(
                postId.toHexString(), "alice@test.com", "Nice post!",
                SentimentType.POSITIVE, 0.9, LocalDateTime.now());

        PaginatedResponse<CommentResponseDto> paged = new PaginatedResponse<>();
        paged.setContent(List.of(dto));
        paged.setPageNumber(0);
        paged.setPageSize(10);
        paged.setFirst(true);
        paged.setLast(true);
        paged.setHasNext(false);

        when(commentService.getCommentByEmail(eq("alice@test.com"), any(Pageable.class)))
                .thenReturn(paged);

        mockMvc.perform(get("/api/comment").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comment fetched successfully"))
                .andExpect(jsonPath("$.data.content[0].text").value("Nice post!"))
                .andExpect(jsonPath("$.data.content[0].userEmail").value("alice@test.com"));
    }

    @Test
    @DisplayName("GET /api/comment → 200 OK with empty list")
    void getAllComments_emptyList_returns200() throws Exception {
        PaginatedResponse<CommentResponseDto> paged = new PaginatedResponse<>();
        paged.setContent(List.of());

        when(commentService.getCommentByEmail(any(), any(Pageable.class))).thenReturn(paged);

        mockMvc.perform(get("/api/comment").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @DisplayName("GET /api/comment → uses authenticated user's email for service call")
    void getAllComments_usesAuthenticatedEmail() throws Exception {
        PaginatedResponse<CommentResponseDto> paged = new PaginatedResponse<>();
        paged.setContent(List.of());
        when(commentService.getCommentByEmail(eq("alice@test.com"), any())).thenReturn(paged);

        mockMvc.perform(get("/api/comment"))
                .andExpect(status().isOk());

        verify(commentService).getCommentByEmail(eq("alice@test.com"), any(Pageable.class));
    }

    // -----------------------------------------------------------------------
    // POST /api/comment
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/comment → 201 CREATED with comment body")
    void createComment_returns201() throws Exception {
        CommentResponseDto created = new CommentResponseDto(
                postId.toHexString(), "alice@test.com", "Great post!",
                null, null, LocalDateTime.now());

        when(commentService.newComment(any(CommentRequest.class), eq("alice@test.com")))
                .thenReturn(created);

        CommentRequest req = new CommentRequest(postId, "Great post!");
        mockMvc.perform(post("/api/comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Great post!"))
                .andExpect(jsonPath("$.userEmail").value("alice@test.com"));
    }

    @Test
    @DisplayName("POST /api/comment → 500 when service throws AccessDeniedException")
    void createComment_accessDenied_returns500() throws Exception {
        when(commentService.newComment(any(), any()))
                .thenThrow(new AccessDeniedException("Private post"));

        CommentRequest req = new CommentRequest(postId, "Hello!");
        mockMvc.perform(post("/api/comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /api/comment → service called with authenticated email")
    void createComment_usesAuthenticatedEmail() throws Exception {
        CommentResponseDto created = new CommentResponseDto();
        when(commentService.newComment(any(), eq("alice@test.com"))).thenReturn(created);

        CommentRequest req = new CommentRequest(postId, "Hello world!");
        mockMvc.perform(post("/api/comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        verify(commentService).newComment(any(CommentRequest.class), eq("alice@test.com"));
    }

    // -----------------------------------------------------------------------
    // DELETE /api/comment/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/comment/{id} → 204 NO_CONTENT on success")
    void deleteComment_returns204() throws Exception {
        doNothing().when(commentService).removeComment(eq(commentId), eq("alice@test.com"));

        mockMvc.perform(delete("/api/comment/{id}", commentId.toHexString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/comment/{id} → service called with correct id and email")
    void deleteComment_passesCorrectArgs() throws Exception {
        doNothing().when(commentService).removeComment(any(ObjectId.class), any());

        mockMvc.perform(delete("/api/comment/{id}", commentId.toHexString()))
                .andExpect(status().isNoContent());

        verify(commentService).removeComment(eq(commentId), eq("alice@test.com"));
    }

    @Test
    @DisplayName("DELETE /api/comment/{id} → 500 when service throws")
    void deleteComment_serviceThrows_returns500() throws Exception {
        doThrow(new RuntimeException("Unexpected"))
                .when(commentService).removeComment(any(ObjectId.class), any());

        mockMvc.perform(delete("/api/comment/{id}", commentId.toHexString()))
                .andExpect(status().isInternalServerError());
    }

    // -----------------------------------------------------------------------
    // PUT /api/comment/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/comment/{id} → 200 OK with updated comment")
    void updateComment_returns200() throws Exception {
        Comment updated = new Comment();
        updated.setId(commentId);
        updated.setText("Updated text");

        when(commentService.updateComment(eq(commentId), any(CommentRequest.class), eq("alice@test.com")))
                .thenReturn(updated);

        CommentRequest req = new CommentRequest(postId, "Updated text");
        mockMvc.perform(put("/api/comment/{id}", commentId.toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Updated text"));
    }

    @Test
    @DisplayName("PUT /api/comment/{id} → 404 when comment not found (via global handler)")
    void updateComment_commentNotFound_returns404() throws Exception {
        when(commentService.updateComment(any(), any(), any()))
                .thenThrow(new CommentNotFoundException("Comment not found"));

        CommentRequest req = new CommentRequest(postId, "Updated text");
        mockMvc.perform(put("/api/comment/{id}", commentId.toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                // CommentNotFoundException has no dedicated handler so falls to 500
                // unless you add one; adjust to 404 when handler is added
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("PUT /api/comment/{id} → service called with authenticated email")
    void updateComment_usesAuthenticatedEmail() throws Exception {
        Comment updated = new Comment();
        when(commentService.updateComment(any(), any(), eq("alice@test.com"))).thenReturn(updated);

        CommentRequest req = new CommentRequest(postId, "Updated text");
        mockMvc.perform(put("/api/comment/{id}", commentId.toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(commentService).updateComment(eq(commentId), any(), eq("alice@test.com"));
    }

    // -----------------------------------------------------------------------
    // GET /api/comment/{postId}/comments/count
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/comment/{postId}/comments/count → 200 OK with count value")
    void getCommentCount_returns200WithCount() throws Exception {
        when(commentService.getCommentCount(postId)).thenReturn(5L);

        mockMvc.perform(get("/api/comment/{postId}/comments/count", postId.toHexString()))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @DisplayName("GET /api/comment/{postId}/comments/count → 200 OK with 0 when no comments")
    void getCommentCount_zeroCount_returns200() throws Exception {
        when(commentService.getCommentCount(postId)).thenReturn(0L);

        mockMvc.perform(get("/api/comment/{postId}/comments/count", postId.toHexString()))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }
}
