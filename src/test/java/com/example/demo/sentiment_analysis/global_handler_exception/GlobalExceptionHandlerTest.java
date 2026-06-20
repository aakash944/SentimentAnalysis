package com.example.demo.sentiment_analysis.global_handler_exception;

import com.example.demo.sentiment_analysis.exception.*;
import com.example.demo.sentiment_analysis.global_handler_exception.exception_dto.ExceptionResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    // =========================================================================
    // PostsNotFoundException → 404
    // =========================================================================

    @Test
    @DisplayName("handlePost: PostsNotFoundException → 404 NOT_FOUND with message")
    void handlePost_returns404() {
        PostsNotFoundException ex = new PostsNotFoundException("Post not found");

        ResponseEntity<ExceptionResponseDto> response = handler.handlePost(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Post not found");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    // =========================================================================
    // UserNotFoundException → 404
    // =========================================================================

    @Test
    @DisplayName("handleUser: UserNotFoundException → 404 NOT_FOUND with message")
    void handleUser_returns404() {
        UserNotFoundException ex = new UserNotFoundException("User not found");

        ResponseEntity<ExceptionResponseDto> response = handler.handleUser(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("User not found");
    }

    // =========================================================================
    // MethodArgumentNotValidException → 400
    // =========================================================================

    @Test
    @DisplayName("handleValidation: MethodArgumentNotValidException → 400 BAD_REQUEST with field errors")
    void handleValidation_returns400() throws Exception {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);

        ResponseEntity<ExceptionResponseDto> response = handler.handleInvalidObjectId(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    // =========================================================================
    // MethodArgumentTypeMismatchException → 400
    // =========================================================================

    @Test
    @DisplayName("handleInvalidObjectId: MethodArgumentTypeMismatchException → 400 BAD_REQUEST")
    void handleInvalidObjectId_returns400() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);

        ResponseEntity<ExceptionResponseDto> response = handler.handleInvalidObjectId(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid ID format");
    }

    // =========================================================================
    // WeakPasswordException → 400
    // =========================================================================

    @Test
    @DisplayName("handleWeakPassword: WeakPasswordException → 400 BAD_REQUEST with message")
    void handleWeakPassword_returns400() {
        WeakPasswordException ex = new WeakPasswordException("Password too weak");

        ResponseEntity<ExceptionResponseDto> response = handler.handleWeakPassword(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Password too weak");
    }

    // =========================================================================
    // RefreshTokenException → 400
    // =========================================================================

    @Test
    @DisplayName("handleRefreshToken: RefreshTokenException → 400 BAD_REQUEST with message")
    void handleRefreshToken_returns400() {
        RefreshTokenException ex = new RefreshTokenException("Refresh token invalid");

        ResponseEntity<ExceptionResponseDto> response = handler.handleRefreshToke(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Refresh token invalid");
    }

    // =========================================================================
    // SentimentAnalysisException → 503
    // =========================================================================

    @Test
    @DisplayName("handleSentimentError: SentimentAnalysisException → 503 SERVICE_UNAVAILABLE")
    void handleSentimentError_returns503() {
        SentimentAnalysisException ex = new SentimentAnalysisException("AI unavailable");

        ResponseEntity<ExceptionResponseDto> response = handler.handleSentimentError(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getStatus()).isEqualTo(503);
        assertThat(response.getBody().getMessage()).isEqualTo("AI unavailable");
    }

    // =========================================================================
    // GibberishCommentException → 400
    // =========================================================================

    @Test
    @DisplayName("handleGibberishComment: GibberishCommentException → 400 BAD_REQUEST")
    void handleGibberishComment_returns400() {
        GibberishCommentException ex = new GibberishCommentException("Comment is gibberish");

        ResponseEntity<ExceptionResponseDto> response = handler.handleGibberishComment(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Comment is gibberish");
    }

    // =========================================================================
    // Generic Exception → 500
    // =========================================================================

    @Test
    @DisplayName("handleAnyOther: generic Exception → 500 INTERNAL_SERVER_ERROR")
    void handleAnyOther_returns500() {
        Exception ex = new RuntimeException("unexpected boom");

        ResponseEntity<ExceptionResponseDto> response = handler.handleAnyOther(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("Something went wrong");
    }

    // helper — Mockito.mock without static import conflict
    private <T> T mock(Class<T> classToMock) {
        return org.mockito.Mockito.mock(classToMock);
    }
}
