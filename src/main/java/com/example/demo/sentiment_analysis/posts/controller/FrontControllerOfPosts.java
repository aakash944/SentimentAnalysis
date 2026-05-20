package com.example.demo.sentiment_analysis.posts.controller;

import com.example.demo.sentiment_analysis.api_response.ApiResponse;
import com.example.demo.sentiment_analysis.dto.PostDto;
import com.example.demo.sentiment_analysis.posts.model.Posts;
import com.example.demo.sentiment_analysis.response_dto.PaginatedResponse;
import com.example.demo.sentiment_analysis.response_dto.posts_response.PostDetailDto;
import com.example.demo.sentiment_analysis.response_dto.posts_response.PostResponseDto;
import com.example.demo.sentiment_analysis.posts.service.LogicOfPosts;
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
@RequestMapping("/api/posts")
public class FrontControllerOfPosts {
    private final LogicOfPosts logicOfSentiment;

    public FrontControllerOfPosts(LogicOfPosts logicOfSentiment) {
        this.logicOfSentiment = logicOfSentiment;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<PostResponseDto>>> getMyPosts(Pageable pageable) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) authentication.getPrincipal();

        PaginatedResponse<PostResponseDto> posts =
                logicOfSentiment.getPostsByUserEmail(principal.getUsername(), pageable);

        ApiResponse<PaginatedResponse<PostResponseDto>> response =
                new ApiResponse<>("Posts fetched successfully", posts, null);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Posts> createPost(@RequestBody PostDto postDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        org.springframework.security.core.userdetails.User principal =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        Posts posts = logicOfSentiment.createPostForUser(postDto, principal.getUsername());
        return new ResponseEntity<>(posts, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable ObjectId id) throws AccessDeniedException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        org.springframework.security.core.userdetails.User principal =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        logicOfSentiment.removePost(id, principal.getUsername());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Posts> updatePost(@PathVariable ObjectId id,
                                            @RequestBody PostDto postDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        org.springframework.security.core.userdetails.User principal =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        Posts posts = logicOfSentiment.updatePost(id, postDto, principal.getUsername());
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailDto>> getPostDetail(@PathVariable ObjectId postId) throws AccessDeniedException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) authentication.getPrincipal();
        PostDetailDto postDetail = logicOfSentiment.getPostDetail(postId, principal.getUsername());

        ApiResponse<PostDetailDto> response = new ApiResponse<>("Post details fetched successfully", postDetail, null);

        return ResponseEntity.ok(response);
    }
}

