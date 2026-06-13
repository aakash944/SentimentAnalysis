package com.example.demo.sentiment_analysis.posts.posts_response;

import com.example.demo.sentiment_analysis.comment.comment_response.CommentResponseDetailDto;
import com.example.demo.sentiment_analysis.reaction.reaction_response.ReactionResponseDetailDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostDetailDto {
    private PostResponseDto post;
    private List<CommentResponseDetailDto> comments;
    private List<ReactionResponseDetailDto> reactions;
}
