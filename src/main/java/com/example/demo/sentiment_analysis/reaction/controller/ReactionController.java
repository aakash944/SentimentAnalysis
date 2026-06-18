package com.example.demo.sentiment_analysis.reaction.controller;

import com.example.demo.sentiment_analysis.error_dto.ApiResponse;
import com.example.demo.sentiment_analysis.reaction.dto.ReactionDto;
import com.example.demo.sentiment_analysis.slice_response_dto.PaginatedResponse;
import com.example.demo.sentiment_analysis.reaction.reaction_response.ReactionResponseDto;
import com.example.demo.sentiment_analysis.reaction.service.ReactionService;
import jakarta.validation.Valid;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;


@RestController
@RequestMapping("/api/react")
public class ReactionController {
    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {

        this.reactionService = reactionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<ReactionResponseDto>>>
    getAllReaction(Pageable pageable) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        User principal =
                (User) authentication.getPrincipal();

        PaginatedResponse<ReactionResponseDto> allReactions =
                reactionService.getAllReactions(
                        principal.getUsername(),
                        pageable
                );
        ApiResponse<PaginatedResponse<ReactionResponseDto>> response =
                new ApiResponse<>("Reaction fetched successfully", allReactions,null);

        return ResponseEntity.ok(response);

    }


    @PostMapping
    public void reactEmoji(@Valid @RequestBody ReactionDto reactionDto) throws AccessDeniedException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        org.springframework.security.core.userdetails.User principal =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        reactionService.createReaction(reactionDto,principal.getUsername());
    }
    @GetMapping("/{postId}/count")
    public ResponseEntity<Long> getReactionCount(@PathVariable ObjectId postId) {
        long count = reactionService.getReactionCount(postId);
        return ResponseEntity.ok(count);
    }
}
