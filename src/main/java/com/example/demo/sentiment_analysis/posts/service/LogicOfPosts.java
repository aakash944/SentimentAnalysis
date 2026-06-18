package com.example.demo.sentiment_analysis.posts.service;

import com.example.demo.sentiment_analysis.posts.post_request_dto.PostDto;
import com.example.demo.sentiment_analysis.posts.enumeration.TypeOfAccess;
import com.example.demo.sentiment_analysis.exception.PostsNotFoundException;
import com.example.demo.sentiment_analysis.exception.UserNotFoundException;
import com.example.demo.sentiment_analysis.posts.model.Posts;
import com.example.demo.sentiment_analysis.redis.service.RedisService;
import com.example.demo.sentiment_analysis.comment.comment_response.CommentResponseDetailDto;
import com.example.demo.sentiment_analysis.posts.posts_response.PostDetailDto;
import com.example.demo.sentiment_analysis.posts.posts_response.PostResponseDto;
import com.example.demo.sentiment_analysis.reaction.reaction_response.ReactionResponseDetailDto;
import com.example.demo.sentiment_analysis.user.model.Users;
import com.example.demo.sentiment_analysis.slice_response_dto.*;
import com.example.demo.sentiment_analysis.comment.repository.CommentRepo;
import com.example.demo.sentiment_analysis.posts.repository.PostsRepo;
import com.example.demo.sentiment_analysis.reaction.repository.ReactionRepo;
import com.example.demo.sentiment_analysis.user.repository.UserRepo;
import com.fasterxml.jackson.core.type.TypeReference;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LogicOfPosts {
    private final PostsRepo postsRepo;
    private final UserRepo userRepo;
    private final CommentRepo commentRepo;
    private final ReactionRepo reactionRepo;
    private final RedisService redisService;

    public LogicOfPosts(PostsRepo postsRepo, UserRepo userRepo, CommentRepo commentRepo, ReactionRepo reactionRepo, RedisService redisService) {
        this.postsRepo = postsRepo;
        this.userRepo = userRepo;
        this.commentRepo = commentRepo;
        this.reactionRepo = reactionRepo;
        this.redisService = redisService;
    }

    public PaginatedResponse<PostResponseDto> getPostsByUserEmail(String userEmail, Pageable pageable) {
        String cacheKey = feedCacheKey(userEmail, pageable);
        PaginatedResponse<PostResponseDto> cachedResponse = redisService.get(cacheKey,
                        new TypeReference<PaginatedResponse<PostResponseDto>>() {
                });

        if (cachedResponse != null) {
            return cachedResponse;
        }

        Users currentUser = userRepo.findByUserEmail(userEmail);

        if (currentUser == null) {
            throw new UsernameNotFoundException("User not found");
        }

        Slice<Posts> slice = postsRepo.findByTypeOrUserId(
                TypeOfAccess.PUBLIC,
                currentUser.getId(),
                pageable
        );

        List<PostResponseDto> content = slice.getContent().stream()
                .map(post -> {
                    Users postOwner = userRepo.findById(post.getUserId()).orElse(null);
                    return new PostResponseDto(
                            post.getId().toHexString(),
                            post.getTitle(),
                            post.getContent(),
                            postOwner != null ? postOwner.getUserEmail() : "unknown",
                            post.getCreateAt(),
                            commentRepo.countByPostId(post.getId()),
                            reactionRepo.countByPostId(post.getId())
                    );
                })
                .toList();

        PaginatedResponse<PostResponseDto> response = new PaginatedResponse<>();
        response.setContent(content);
        response.setPageNumber(slice.getNumber());
        response.setPageSize(slice.getSize());
        response.setFirst(slice.isFirst());
        response.setLast(!slice.hasNext());
        response.setHasNext(slice.hasNext());

        // cache for short time
        redisService.set(cacheKey, response, 30L);

        return response;
    }

    @Transactional
    public Posts createPostForUser(PostDto postDto, String currentUserEmail) {
        Users currentUser = userRepo.findByUserEmail(currentUserEmail);
        Posts posts = new Posts();
        posts.setType(postDto.getTypeOfAccess());
        posts.setUserId(currentUser.getId());
        posts.setContent(postDto.getContent());
        posts.setTitle(postDto.getTitle());
        posts.setCreateAt(LocalDateTime.now());

        return postsRepo.save(posts);
    }

    public void removePost(ObjectId id, String userEmail) throws AccessDeniedException {

        Users currentUser = userRepo.findByUserEmail(userEmail);
        Posts post = postsRepo.findById(id)
                .orElseThrow(() ->
                        new PostsNotFoundException("Post not found"));

        // OWNERSHIP CHECK
        if (!post.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException(
                    "You can delete only your own post"
            );
        }

        // delete related comments
        commentRepo.deleteByPostId(post.getId());

        // delete related reactions
        reactionRepo.deleteByPostId(post.getId());

        // delete post
        postsRepo.delete(post);
    }

    public Posts updatePost(ObjectId id, PostDto postDto, String userEmail) {
        Users currentUser = userRepo.findByUserEmail(userEmail);
        Posts posts = postsRepo.findById(id)
                .orElseThrow(() -> new PostsNotFoundException("Post is not found"));
        if (!posts.getUserId().equals(currentUser.getId())) {
            throw new UserNotFoundException("User not found exception");
        }

        if (postDto.getContent() != null && !postDto.getContent().isEmpty()) {
            posts.setContent(postDto.getContent());
        }

        if (postDto.getTitle() != null && !postDto.getTitle().isEmpty()) {
            posts.setTitle(postDto.getTitle());
        }

        posts.setCreateAt(LocalDateTime.now());

        return postsRepo.save(posts);
    }

    public PostDetailDto getPostDetail(ObjectId postId, String userEmail) throws AccessDeniedException {

        Users currentUser = userRepo.findByUserEmail(userEmail);
        if (currentUser == null) {
            throw new UserNotFoundException("User not found");
        }

        Posts post = postsRepo.findById(postId)
                .orElseThrow(() -> new PostsNotFoundException("Post not found"));

        boolean canSee =
                post.getType() == TypeOfAccess.PUBLIC ||
                        post.getUserId().equals(currentUser.getId());

        if (!canSee) {
            throw new AccessDeniedException("You cannot view this post");
        }

        Users postOwner = userRepo.findById(post.getUserId()).orElse(null);
        String authorEmail = postOwner != null ? postOwner.getUserEmail() : "unknown";

        long commentCount = commentRepo.countByPostId(postId);
        long reactionCount = reactionRepo.countByPostId(postId);

        PostResponseDto postDto = new PostResponseDto(
                post.getId().toHexString(),
                post.getTitle(),
                post.getContent(),
                authorEmail,
                post.getCreateAt(),
                commentCount,
                reactionCount
        );

        List<CommentResponseDetailDto> commentDtos = commentRepo.findByPostId(postId).stream()
                .map(comment -> {
                    Users commentUser = userRepo.findById(comment.getUserId()).orElse(null);

                    return new CommentResponseDetailDto(
                            comment.getText(),
                            commentUser != null ? commentUser.getUserEmail() : "unknown",
                            comment.getUpdatedAt()
                    );
                })
                .toList();

        List<ReactionResponseDetailDto> reactionDtos = reactionRepo.findByPostId(postId).stream()
                .map(reaction -> {
                    Users reactionUser = userRepo.findById(reaction.getUserId()).orElse(null);

                    return new ReactionResponseDetailDto(
                            reaction.getReactionType().name(),
                            reactionUser != null ? reactionUser.getUserEmail() : "unknown",
                            reaction.getCreatedAt()
                    );
                })
                .toList();

        return new PostDetailDto(postDto, commentDtos, reactionDtos);
    }

    private String feedCacheKey(String userEmail, Pageable pageable) {
        return "feed:" + userEmail + ":page:" + pageable.getPageNumber() + ":size:" + pageable.getPageSize();
    }
}
