package com.example.demo.sentiment_analysis.user.controller;


import com.example.demo.sentiment_analysis.user.user_response.CurrentUserResponse;
import com.example.demo.sentiment_analysis.user.dto.UserDto;
import com.example.demo.sentiment_analysis.user.model.Users;
import com.example.demo.sentiment_analysis.user.service.UserService;

import org.bson.types.ObjectId;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) authentication.getPrincipal();
        Users user = userService.getCurrentUser(principal.getUsername());
        CurrentUserResponse response = new CurrentUserResponse(
                user.getId().toHexString(),
                user.getUserEmail(),
                user.getRoles()
        );
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable ObjectId id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) authentication.getPrincipal();
        userService.removeUser(id, principal.getUsername());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Users> updateUser(@PathVariable ObjectId id,
                                            @RequestBody UserDto userInfo) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) authentication.getPrincipal();
        Users updated = userService.newUserUpdate(id, userInfo, principal.getUsername());
        return ResponseEntity.ok(updated);
    }
}
