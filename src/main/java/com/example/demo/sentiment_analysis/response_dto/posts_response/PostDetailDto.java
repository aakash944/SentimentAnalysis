package com.example.demo.sentiment_analysis.response_dto.posts_response;

import com.example.demo.sentiment_analysis.response_dto.comment_response.commentResponseDetailDto;
import com.example.demo.sentiment_analysis.response_dto.reaction_response.ReactionResponseDetailDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostDetailDto {
    private PostResponseDto post;
    private List<commentResponseDetailDto> comments;
    private List<ReactionResponseDetailDto> reactions;
}
