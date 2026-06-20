package com.example.demo.sentiment_analysis.reaction.dto;

import com.example.demo.sentiment_analysis.reaction.enumeration.ReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReactionDto {
    @NotNull
    private ObjectId postId;
    @NotNull
    private ReactionType reactionType;
}
