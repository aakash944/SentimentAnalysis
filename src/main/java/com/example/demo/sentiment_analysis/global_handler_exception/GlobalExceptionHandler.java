package com.example.demo.sentiment_analysis.global_handler_exception;


import com.example.demo.sentiment_analysis.exception.PostsNotFoundException;
import com.example.demo.sentiment_analysis.exception.RefreshTokenException;
import com.example.demo.sentiment_analysis.exception.UserNotFoundException;
import com.example.demo.sentiment_analysis.exception.WeakPasswordException;
import com.example.demo.sentiment_analysis.global_handler_exception.exception_dto.ExceptionResponseDto;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PostsNotFoundException.class)
    public ResponseEntity<ExceptionResponseDto> handlePost(PostsNotFoundException ex, HttpServletRequest request
    ) {
        ExceptionResponseDto error = new ExceptionResponseDto(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ExceptionResponseDto> handleUser(
            UserNotFoundException ex,
            HttpServletRequest request
    ) {
        ExceptionResponseDto error = new ExceptionResponseDto(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ExceptionResponseDto> handleInvalidObjectId(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        ExceptionResponseDto error = new ExceptionResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid ID format",
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<ExceptionResponseDto> handleWeakPassword(
            WeakPasswordException ex,
            HttpServletRequest request
    ) {
        ExceptionResponseDto error = new ExceptionResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(RefreshTokenException.class)
    public ResponseEntity<ExceptionResponseDto> handleRefreshToke(RefreshTokenException ex, HttpServletRequest request) {
        ExceptionResponseDto error = new ExceptionResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponseDto> handleAnyOther(
//            Exception ex,
//            HttpServletRequest request
//    ) {
//        ErrorResponseDto error = new ErrorResponseDto(
//                HttpStatus.INTERNAL_SERVER_ERROR.value(),
//                "Something went wrong",
//                LocalDateTime.now(),
//                request.getRequestURI()
//        );
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
//    }
}
