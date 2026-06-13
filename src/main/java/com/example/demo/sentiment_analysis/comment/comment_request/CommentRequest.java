package com.example.demo.sentiment_analysis.comment.comment_request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentRequest {
    private ObjectId postId;
    private String text;
}
