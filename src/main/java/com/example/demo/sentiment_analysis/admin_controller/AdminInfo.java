package com.example.demo.sentiment_analysis.admin_controller;

import com.example.demo.sentiment_analysis.api_response.ApiResponse;
import com.example.demo.sentiment_analysis.response_dto.PaginatedResponse;
import com.example.demo.sentiment_analysis.response_dto.user_response.UserResponse;
import com.example.demo.sentiment_analysis.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.domain.Pageable;


@RestController
@RequestMapping("/Admin_userInfo")

public class AdminInfo {
    private final UserService userService;

    public AdminInfo(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/getAllUser")
    public ResponseEntity<ApiResponse<PaginatedResponse<UserResponse>>> getAllUser(Pageable pageable) {

        PaginatedResponse<UserResponse> users = userService.getUserDb(pageable);

        ApiResponse<PaginatedResponse<UserResponse>> response =
                new ApiResponse<>("Users fetched successfully", users, null);

        return ResponseEntity.ok(response);
    }
}