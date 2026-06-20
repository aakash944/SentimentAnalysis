package com.example.demo.sentiment_analysis.comment.comment_request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentRequest {
    @NotNull
    private ObjectId postId;
    @NotBlank
    private String text;
}
