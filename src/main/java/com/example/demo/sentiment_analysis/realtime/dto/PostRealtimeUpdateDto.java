package com.example.demo.sentiment_analysis.realtime.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostRealtimeUpdateDto {
    private String postId;
    private String eventType;
    private String actorEmail;
    private long commentCount;
    private long reactionCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime emittedAt;
}
