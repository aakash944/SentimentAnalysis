package com.example.demo.sentiment_analysis.request_dto;

import com.example.demo.sentiment_analysis.enumeration.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReactionDto {
    private ObjectId postId;   // which post
    private ReactionType reactionType;
}
