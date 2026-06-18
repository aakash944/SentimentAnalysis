package com.example.demo.sentiment_analysis.comment.comment_request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentRequest {
    @NotBlank
    private ObjectId postId;
    @NotBlank
    private String text;
}
