package com.example.demo.sentiment_analysis.reaction.dto;

import com.example.demo.sentiment_analysis.reaction.enumeration.ReactionType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReactionDto {
    @NotBlank
    private ObjectId postId;   // which post
    @NotBlank
    private ReactionType reactionType;
}
