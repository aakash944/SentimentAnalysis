package com.example.demo.sentiment_analysis.reaction.service;

import com.example.demo.sentiment_analysis.dto.ReactionDto;
import com.example.demo.sentiment_analysis.enumeration.TypeOfAccess;
import com.example.demo.sentiment_analysis.exception.PostsNotFoundException;
import com.example.demo.sentiment_analysis.posts.model.Posts;
import com.example.demo.sentiment_analysis.reaction.model.Reaction;
import com.example.demo.sentiment_analysis.reaction.repository.ReactionRepo;
import com.example.demo.sentiment_analysis.user.model.Users;
import com.example.demo.sentiment_analysis.response_dto.PaginatedResponse;
import com.example.demo.sentiment_analysis.posts.repository.PostsRepo;
import com.example.demo.sentiment_analysis.user.repository.UserRepo;
import com.example.demo.sentiment_analysis.response_dto.reaction_response.ReactionResponseDto;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReactionService {

    private final ReactionRepo reactionRepo;
    private final UserRepo userRepo;
    private final PostsRepo postsRepo;

    public ReactionService(ReactionRepo reactionRepo, UserRepo userRepo, PostsRepo postsRepo) {
        this.reactionRepo = reactionRepo;
        this.userRepo = userRepo;
        this.postsRepo = postsRepo;
    }

    public PaginatedResponse<ReactionResponseDto> getAllReactions(
            String userEmail,
            Pageable pageable
    ) {

        Users user = userRepo.findByUserEmail(userEmail);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        List<Posts> allPosts = postsRepo.findAll();

        List<ObjectId> visiblePostIds = allPosts.stream()
                .filter(post ->
                        post.getType() == TypeOfAccess.PUBLIC
                                || post.getUserId().equals(user.getId())
                )
                .map(Posts::getId)
                .toList();

        Slice<Reaction> slice = reactionRepo.findByPostIdIn(visiblePostIds, pageable);

        List<ReactionResponseDto> content = slice.getContent().stream()
                .map(reaction -> {
                    Users reactionUser = userRepo.findById(reaction.getUserId())
                            .orElse(null);

                    return new ReactionResponseDto(
                            reaction.getPostId().toHexString(),
                            reactionUser != null ? reactionUser.getUserEmail() : "unknown",
                            reaction.getReactionType(),
                            reaction.getCreatedAt()
                    );
                })
                .toList();

        PaginatedResponse<ReactionResponseDto> response = new PaginatedResponse<>();
        response.setContent(content);
        response.setPageNumber(slice.getNumber());
        response.setPageSize(slice.getSize());
        response.setFirst(slice.isFirst());
        response.setLast(!slice.hasNext());
        response.setHasNext(slice.hasNext());

        return response;
    }

    public void createReaction(ReactionDto dto, String userEmail) throws AccessDeniedException {

        Users user = userRepo.findByUserEmail(userEmail);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        Posts post = postsRepo.findById(dto.getPostId())
                .orElseThrow(() -> new PostsNotFoundException("Post not found"));

        // ACCESS RULE (your requirement)
        boolean allowed =
                post.getType() == TypeOfAccess.PUBLIC ||
                        post.getUserId().equals(user.getId());

        if (!allowed) {
            throw new AccessDeniedException("Cannot react on private post");
        }

        Optional<Reaction> existing =
                reactionRepo.findByUserIdAndPostId(user.getId(), post.getId());

        if (existing.isPresent()) {
            Reaction r = existing.get();

            if (r.getReactionType().equals(dto.getReactionType())) {
                reactionRepo.delete(r);
                return;
            }

            r.setReactionType(dto.getReactionType());
            r.setCreatedAt(LocalDateTime.now());
            reactionRepo.save(r);
            return;
        }

        Reaction reaction = new Reaction();
        reaction.setUserId(user.getId());
        reaction.setPostId(post.getId());
        reaction.setReactionType(dto.getReactionType());
        reaction.setCreatedAt(LocalDateTime.now());

        reactionRepo.save(reaction);
    }
}