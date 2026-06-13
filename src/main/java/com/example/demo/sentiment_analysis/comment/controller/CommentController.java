package com.example.demo.sentiment_analysis.comment.controller;

import com.example.demo.sentiment_analysis.error_dto.ApiResponse;
import com.example.demo.sentiment_analysis.comment.comment_request.CommentRequest;
import com.example.demo.sentiment_analysis.comment.model.Comment;
import com.example.demo.sentiment_analysis.comment.service.CommentService;
import com.example.demo.sentiment_analysis.comment.comment_response.CommentResponseDto;
import com.example.demo.sentiment_analysis.slice_response_dto.PaginatedResponse;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@RestController
@RequestMapping("/api/comment")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }
    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<CommentResponseDto>>> getAllCommentOfUser(Pageable pageable) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) authentication.getPrincipal();

        PaginatedResponse<CommentResponseDto> comments =
                commentService.getCommentByEmail(principal.getUsername(), pageable);
        ApiResponse<PaginatedResponse<CommentResponseDto>> response =
                new ApiResponse<>("Comment fetched successfully", comments, null);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(@RequestBody CommentRequest commentDto) throws AccessDeniedException {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) authentication.getPrincipal();
        CommentResponseDto commentCreated = commentService.newComment(commentDto, principal.getUsername());
        return new ResponseEntity<>(commentCreated, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable ObjectId id) throws AccessDeniedException {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) authentication.getPrincipal();
        commentService.removeComment(id, principal.getUsername());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Comment> updateComment(@PathVariable ObjectId id,
                                                 @RequestBody CommentRequest commentDto) throws AccessDeniedException {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) authentication.getPrincipal();
        Comment comment = commentService
                .updateComment(id, commentDto, principal.getUsername());
        return new ResponseEntity<>(comment, HttpStatus.OK);
    }

    @GetMapping("/{postId}/comments/count")
    public ResponseEntity<Long> getCommentCount(@PathVariable ObjectId postId) {
        long count = commentService.getCommentCount(postId);
        return ResponseEntity.ok(count);
    }
}
