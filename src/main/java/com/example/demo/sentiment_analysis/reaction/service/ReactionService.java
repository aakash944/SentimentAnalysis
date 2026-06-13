package com.example.demo.sentiment_analysis.reaction.service;

import com.example.demo.sentiment_analysis.reaction.dto.ReactionDto;
import com.example.demo.sentiment_analysis.enumeration.TypeOfAccess;
import com.example.demo.sentiment_analysis.exception.PostsNotFoundException;
import com.example.demo.sentiment_analysis.posts.model.Posts;
import com.example.demo.sentiment_analysis.reaction.model.Reaction;
import com.example.demo.sentiment_analysis.reaction.repository.ReactionRepo;
import com.example.demo.sentiment_analysis.redis.service.RedisService;
import com.example.demo.sentiment_analysis.user.model.Users;
import com.example.demo.sentiment_analysis.slice_response_dto.PaginatedResponse;
import com.example.demo.sentiment_analysis.posts.repository.PostsRepo;
import com.example.demo.sentiment_analysis.user.repository.UserRepo;
import com.example.demo.sentiment_analysis.realtime_websocket.service.PostRealtimePublisher;
import com.example.demo.sentiment_analysis.reaction.reaction_response.ReactionResponseDto;
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
    private final PostRealtimePublisher postRealtimePublisher;
    private final RedisService redisService;

    public ReactionService(ReactionRepo reactionRepo, UserRepo userRepo, PostsRepo postsRepo, PostRealtimePublisher postRealtimePublisher, RedisService redisService) {
        this.reactionRepo = reactionRepo;
        this.userRepo = userRepo;
        this.postsRepo = postsRepo;
        this.postRealtimePublisher = postRealtimePublisher;
        this.redisService = redisService;
    }

    public PaginatedResponse<ReactionResponseDto> getAllReactions(String userEmail, Pageable pageable) {
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

                redisService.decrement(reactionCountKey(post.getId()));

                postRealtimePublisher.publishPostUpdate(
                        post.getId(),
                        "REACTION_DELETED",
                        userEmail
                );
                return;
            }

            r.setReactionType(dto.getReactionType());
            r.setCreatedAt(LocalDateTime.now());
            reactionRepo.save(r);
            postRealtimePublisher.publishPostUpdate(post.getId(),
                    "REACTION_UPDATED", userEmail);
            return;
        }

        Reaction reaction = new Reaction();
        reaction.setUserId(user.getId());
        reaction.setPostId(post.getId());
        reaction.setReactionType(dto.getReactionType());
        reaction.setCreatedAt(LocalDateTime.now());

        reactionRepo.save(reaction);

        redisService.increment(reactionCountKey(post.getId()));

        postRealtimePublisher.publishPostUpdate(
                post.getId(),
                "REACTION_CREATED",
                userEmail
        );
    }

    private String reactionCountKey(ObjectId postId) {

        return "reaction:count:" + postId.toHexString();
    }
    public long getReactionCount(ObjectId postId) {

        String key = reactionCountKey(postId);

        Long cached = redisService.getLong(key);
        if (cached != null) {
            return cached;
        }

        long dbCount = reactionRepo.countByPostId(postId);

        redisService.setLong(key, dbCount, null);

        return dbCount;
    }
}
