package com.example.demo.sentiment_analysis.comment.comment_response;

import com.example.demo.sentiment_analysis.ai.enumeration.SentimentType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponseDto {
    String id;
    private String postId;
    private String userEmail;
    private String text;
    private SentimentType sentiment;
    private Double confidence;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createAt;
}
