package com.example.demo.sentiment_analysis.response_dto.reaction_response;

import com.example.demo.sentiment_analysis.enumeration.ReactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReactionResponseDto {
    private String postId;

    private String userEmail;

    private ReactionType reactionType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
