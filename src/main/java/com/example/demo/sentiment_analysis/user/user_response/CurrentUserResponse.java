package com.example.demo.sentiment_analysis.user.user_response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrentUserResponse {
    private String id;
    private String userEmail;
    private List<String> roles;
}

