package com.example.demo.sentiment_analysis.user.controller;


import com.example.demo.sentiment_analysis.user.dto.UserDto;
import com.example.demo.sentiment_analysis.user.model.Users;
import com.example.demo.sentiment_analysis.user.service.UserService;

import org.bson.types.ObjectId;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable ObjectId id) {
        userService.removeUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Users> updateUser(@PathVariable ObjectId id,
                                            @RequestBody UserDto userInfo) {

        Users updatedUser = userService.newUserUpdate(id, userInfo);
        return ResponseEntity.ok(updatedUser);
    }
}
