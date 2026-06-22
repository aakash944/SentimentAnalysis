//package com.example.demo.sentiment_analysis.comment.service;
//
//
//import com.example.demo.sentiment_analysis.comment.comment_request.CommentRequest;
//import com.example.demo.sentiment_analysis.exception.GibberishCommentException;
//import com.example.demo.sentiment_analysis.posts.enumeration.TypeOfAccess;
//import com.example.demo.sentiment_analysis.exception.CommentNotFoundException;
//import com.example.demo.sentiment_analysis.exception.PostsNotFoundException;
//import com.example.demo.sentiment_analysis.comment.repository.CommentRepo;
//import com.example.demo.sentiment_analysis.comment.model.Comment;
//import com.example.demo.sentiment_analysis.exception.UserNotFoundException;
//import com.example.demo.sentiment_analysis.posts.model.Posts;
//import com.example.demo.sentiment_analysis.redis.service.RedisService;
//import com.example.demo.sentiment_analysis.user.model.Users;
//import com.example.demo.sentiment_analysis.realtime_websocket.service.PostRealtimePublisher;
//import com.example.demo.sentiment_analysis.comment.comment_response.CommentResponseDto;
//import com.example.demo.sentiment_analysis.slice_response_dto.PaginatedResponse;
//import com.example.demo.sentiment_analysis.posts.repository.PostsRepo;
//import com.example.demo.sentiment_analysis.user.repository.UserRepo;
//import lombok.extern.slf4j.Slf4j;
//import org.bson.types.ObjectId;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Slice;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.nio.file.AccessDeniedException;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Slf4j
//@Service
//public class CommentService {
//    private final CommentRepo commentRepo;
//    private final UserRepo userRepo;
//    private final PostsRepo postsRepo;
//    private final PostRealtimePublisher postRealtimePublisher;
//    private final RedisService redisService;
//    private final KafkaTemplate<String, String> kafkaTemplate;
//
//    public CommentService(CommentRepo commentRepo, UserRepo userRepo, PostsRepo postsRepo, PostRealtimePublisher postRealtimePublisher, RedisService redisService, KafkaTemplate<String, String> kafkaTemplate) {
//        this.commentRepo = commentRepo;
//        this.userRepo = userRepo;
//        this.postsRepo = postsRepo;
//        this.postRealtimePublisher = postRealtimePublisher;
//        this.redisService = redisService;
//        this.kafkaTemplate = kafkaTemplate;
//    }
//
//    public PaginatedResponse<CommentResponseDto> getCommentByEmail(String userEmail, Pageable pageable) {
//
//        Users user = userRepo.findByUserEmail(userEmail);
//        if (user == null) {
//            throw new UserNotFoundException("User not found");
//        }
//
//        List<ObjectId> visiblePostIds = postsRepo.findAll().stream()
//                .filter(post ->
//                        post.getType() == TypeOfAccess.PUBLIC ||
//                                post.getUserId().equals(user.getId())
//                )
//                .map(Posts::getId)
//                .toList();
//
//        Slice<Comment> slice = commentRepo.findByPostIdIn(visiblePostIds, pageable);
//
//        List<CommentResponseDto> content = slice.getContent().stream()
//                .map(comment -> {
//                    Users commentUser = userRepo.findById(comment.getUserId())
//                            .orElse(null);
//
//                    return new CommentResponseDto(
//                            comment.getId().toHexString(),
//                            comment.getPostId().toHexString(),
//                            commentUser != null ? commentUser.getUserEmail() : "unknown",
//                            comment.getText(),
//                            comment.getSentiment(),
//                            comment.getConfidence(),
//                            comment.getUpdatedAt()
//                    );
//                })
//                .toList();
//
//        PaginatedResponse<CommentResponseDto> response = new PaginatedResponse<>();
//        response.setContent(content);
//        response.setPageNumber(slice.getNumber());
//        response.setPageSize(slice.getSize());
//        response.setFirst(slice.isFirst());
//        response.setLast(!slice.hasNext());
//        response.setHasNext(slice.hasNext());
//
//        return response;
//    }
//
////    @Transactional
//    public CommentResponseDto newComment(CommentRequest commentDto, String userEmail) throws AccessDeniedException {
//        try {
//            Users currentUser = userRepo.findByUserEmail(userEmail);
//            if (currentUser == null) {
//                throw new UserNotFoundException("User not found");
//            }
//            Posts post = postsRepo.findById(commentDto.getPostId())
//                    .orElseThrow(() -> new PostsNotFoundException("Posts not found "));
//
//            boolean canAccess = post.getType() == TypeOfAccess.PUBLIC ||
//                    post.getUserId().equals(currentUser.getId());
//
//            if (!canAccess) {
//                throw new AccessDeniedException("You cannot comment on this post");
//            }
//
//            // Validate comment text
//            String text = commentDto.getText().trim();
//            if (text.isEmpty() || text.length() < 3) {
//                throw new GibberishCommentException("Comment too short");
//            }
//
//            Comment comment = new Comment();
//            comment.setUserId(currentUser.getId());
//            comment.setPostId(post.getId());
//            comment.setText(commentDto.getText());
//            comment.setUpdatedAt(LocalDateTime.now());
//
//            Comment savedComment = commentRepo.save(comment);
//            log.info("SAVING COMMENT ID: {}", savedComment.getId());
//            kafkaTemplate.send("comments-topic", savedComment.getId().toHexString(), savedComment.getId().toHexString() + "::"
//                    + savedComment.getText());
//
//
//            String countKey = "comment:count:" + post.getId().toHexString();
//            redisService.increment(countKey);
//            postRealtimePublisher.publishPostUpdate(post.getId(),
//                    "COMMENT_CREATED", userEmail);
//
//            return new CommentResponseDto(
//                    savedComment.getId().toHexString(),
//                    savedComment.getPostId().toHexString(),
//                    currentUser.getUserEmail(),
//                    savedComment.getText(),
//                    savedComment.getSentiment(),
//                    savedComment.getConfidence(),
//                    savedComment.getUpdatedAt()
//            );
//
//        } catch (Exception e) {
//            log.error("Not created comment from {} ", userEmail, e);
//                throw e;
//        }
//    }
//
//    public void removeComment(ObjectId id, String userEmail) throws AccessDeniedException {
//        try {
//            Users user = userRepo.findByUserEmail(userEmail);
//            if (user == null) {
//                throw new UserNotFoundException("User not found");
//            }
//            Comment comment = commentRepo.findById(id)
//                    .orElseThrow(() -> new CommentNotFoundException("Comment not found"));
//
//            Posts post = postsRepo.findById(comment.getPostId())
//                    .orElseThrow(() -> new PostsNotFoundException("Post not found"));
//
//            boolean canDelete = post.getUserId().equals(user.getId()) ||
//                    comment.getUserId().equals(user.getId());
//
//            if (!canDelete) {
//                throw new AccessDeniedException("Not allowed to delete this comment");
//            }
//
//            commentRepo.delete(comment);
//
//            redisService.decrement(commentCountKey(post.getId()));
//
//            postRealtimePublisher.publishPostUpdate(
//                    post.getId(),
//                    "COMMENT_DELETED",
//                    userEmail
//            );
//        } catch (Exception e) {
//            log.error("Failed to delete comment {} by user {}", id, userEmail, e);
//            throw e;
//        }
//    }
//
//    public Comment updateComment(ObjectId id, CommentRequest commentDto, String userEmail) throws AccessDeniedException {
//        try {
//            Users user = userRepo.findByUserEmail(userEmail);
//            if (user == null) {
//                throw new UserNotFoundException("User not found");
//            }
//            Comment existingComment = commentRepo.findById(id)
//                    .orElseThrow(() -> new CommentNotFoundException("Comment not found"));
//
//            Posts post = postsRepo.findById(existingComment.getPostId())
//                    .orElseThrow(() -> new PostsNotFoundException("Post not found"));
//
//
//            if (!existingComment.getUserId().equals(user.getId())) {
//                throw new AccessDeniedException("You can only update your own comment");
//            }
//
//            boolean canAccess =
//                    post.getType() == TypeOfAccess.PUBLIC ||
//                            post.getUserId().equals(user.getId());
//
//            if (!canAccess) {
//                throw new AccessDeniedException("Post not accessible");
//            }
//
//            if (commentDto.getText() != null && !commentDto.getText().isBlank()) {
//                existingComment.setText(commentDto.getText());
//            }
//            existingComment.setUpdatedAt(LocalDateTime.now());
//            return commentRepo.save(existingComment);
//        } catch (Exception e) {
//            log.error("Failed to update comment {} by user {}", id, userEmail, e);
//            throw e;
//        }
//    }
//
//    private String commentCountKey(ObjectId postId) {
//        return "comment:count:" + postId.toHexString();
//    }
//
//    public long getCommentCount(ObjectId postId) {
//        String key = commentCountKey(postId);
//        Long cached = redisService.getLong(key);
//        if (cached != null) {
//            log.info("CACHE HIT");
//            return cached;
//        }
//        log.info("CACHE MISS");
//        long dbCount = commentRepo.countByPostId(postId);
//        redisService.setLong(key, dbCount, null);
//        return dbCount;
//    }
//}
//
package com.example.demo.sentiment_analysis.comment.service;


import com.example.demo.sentiment_analysis.comment.comment_request.CommentRequest;
import com.example.demo.sentiment_analysis.exception.GibberishCommentException;
import com.example.demo.sentiment_analysis.posts.enumeration.TypeOfAccess;
import com.example.demo.sentiment_analysis.exception.CommentNotFoundException;
import com.example.demo.sentiment_analysis.exception.PostsNotFoundException;
import com.example.demo.sentiment_analysis.comment.repository.CommentRepo;
import com.example.demo.sentiment_analysis.comment.model.Comment;
import com.example.demo.sentiment_analysis.exception.UserNotFoundException;
import com.example.demo.sentiment_analysis.posts.model.Posts;
import com.example.demo.sentiment_analysis.redis.service.RedisService;
import com.example.demo.sentiment_analysis.user.model.Users;
import com.example.demo.sentiment_analysis.realtime_websocket.service.PostRealtimePublisher;
import com.example.demo.sentiment_analysis.comment.comment_response.CommentResponseDto;
import com.example.demo.sentiment_analysis.slice_response_dto.PaginatedResponse;
import com.example.demo.sentiment_analysis.posts.repository.PostsRepo;
import com.example.demo.sentiment_analysis.user.repository.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CommentService {
    private final CommentRepo commentRepo;
    private final UserRepo userRepo;
    private final PostsRepo postsRepo;
    private final PostRealtimePublisher postRealtimePublisher;
    private final RedisService redisService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public CommentService(CommentRepo commentRepo, UserRepo userRepo, PostsRepo postsRepo, PostRealtimePublisher postRealtimePublisher, RedisService redisService, KafkaTemplate<String, String> kafkaTemplate) {
        this.commentRepo = commentRepo;
        this.userRepo = userRepo;
        this.postsRepo = postsRepo;
        this.postRealtimePublisher = postRealtimePublisher;
        this.redisService = redisService;
        this.kafkaTemplate = kafkaTemplate;
    }

    public PaginatedResponse<CommentResponseDto> getCommentByEmail(String userEmail, Pageable pageable) {

        Users user = userRepo.findByUserEmail(userEmail);
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }

        List<ObjectId> visiblePostIds = postsRepo.findAll().stream()
                .filter(post ->
                        post.getType() == TypeOfAccess.PUBLIC ||
                                post.getUserId().equals(user.getId())
                )
                .map(Posts::getId)
                .toList();

        Slice<Comment> slice = commentRepo.findByPostIdIn(visiblePostIds, pageable);

        List<CommentResponseDto> content = slice.getContent().stream()
                .map(comment -> {
                    Users commentUser = userRepo.findById(comment.getUserId())
                            .orElse(null);

                    return new CommentResponseDto(
                            comment.getId().toHexString(),
                            comment.getPostId().toHexString(),
                            commentUser != null ? commentUser.getUserEmail() : "unknown",
                            comment.getText(),
                            comment.getSentiment(),
                            comment.getConfidence(),
                            comment.getUpdatedAt()
                    );
                })
                .toList();

        PaginatedResponse<CommentResponseDto> response = new PaginatedResponse<>();
        response.setContent(content);
        response.setPageNumber(slice.getNumber());
        response.setPageSize(slice.getSize());
        response.setFirst(slice.isFirst());
        response.setLast(!slice.hasNext());
        response.setHasNext(slice.hasNext());

        return response;
    }

    //    @Transactional
    public CommentResponseDto newComment(CommentRequest commentDto, String userEmail) throws AccessDeniedException {
        try {
            Users currentUser = userRepo.findByUserEmail(userEmail);
            if (currentUser == null) {
                throw new UserNotFoundException("User not found");
            }
            Posts post = postsRepo.findById(commentDto.getPostId())
                    .orElseThrow(() -> new PostsNotFoundException("Posts not found "));

            boolean canAccess = post.getType() == TypeOfAccess.PUBLIC ||
                    post.getUserId().equals(currentUser.getId());

            if (!canAccess) {
                throw new AccessDeniedException("You cannot comment on this post");
            }

            // Validate comment text
            String text = commentDto.getText().trim();
            if (text.isEmpty() || text.length() < 3) {
                throw new GibberishCommentException("Comment too short");
            }

            Comment comment = new Comment();
            comment.setUserId(currentUser.getId());
            comment.setPostId(post.getId());
            comment.setText(commentDto.getText());
            comment.setUpdatedAt(LocalDateTime.now());

            Comment savedComment = commentRepo.save(comment);
            log.info("SAVING COMMENT ID: {}", savedComment.getId());
            kafkaTemplate.send("comments-topic", savedComment.getId().toHexString(), savedComment.getId().toHexString() + "::"
                    + savedComment.getText());


            String countKey = "comment:count:" + post.getId().toHexString();
            redisService.increment(countKey);
            postRealtimePublisher.publishPostUpdate(post.getId(),
                    "COMMENT_CREATED", userEmail);

            return new CommentResponseDto(
                    savedComment.getId().toHexString(),
                    savedComment.getPostId().toHexString(),
                    currentUser.getUserEmail(),
                    savedComment.getText(),
                    savedComment.getSentiment(),
                    savedComment.getConfidence(),
                    savedComment.getUpdatedAt()
            );

        } catch (Exception e) {
            log.error("Not created comment from {} ", userEmail, e);
            throw e;
        }
    }

    public void removeComment(ObjectId id, String userEmail) throws AccessDeniedException {
        try {
            Users user = userRepo.findByUserEmail(userEmail);
            if (user == null) {
                throw new UserNotFoundException("User not found");
            }
            Comment comment = commentRepo.findById(id)
                    .orElseThrow(() -> new CommentNotFoundException("Comment not found"));

            Posts post = postsRepo.findById(comment.getPostId())
                    .orElseThrow(() -> new PostsNotFoundException("Post not found"));

            boolean canDelete = post.getUserId().equals(user.getId()) ||
                    comment.getUserId().equals(user.getId());

            if (!canDelete) {
                throw new AccessDeniedException("Not allowed to delete this comment");
            }

            commentRepo.delete(comment);

            redisService.decrement(commentCountKey(post.getId()));

            postRealtimePublisher.publishPostUpdate(
                    post.getId(),
                    "COMMENT_DELETED",
                    userEmail
            );
        } catch (Exception e) {
            log.error("Failed to delete comment {} by user {}", id, userEmail, e);
            throw e;
        }
    }

    public Comment updateComment(ObjectId id, CommentRequest commentDto, String userEmail) throws AccessDeniedException {
        try {
            Users user = userRepo.findByUserEmail(userEmail);
            if (user == null) {
                throw new UsernameNotFoundException("User not found");
            }
            Comment existingComment = commentRepo.findById(id)
                    .orElseThrow(() -> new CommentNotFoundException("Comment not found"));

            Posts post = postsRepo.findById(existingComment.getPostId())
                    .orElseThrow(() -> new PostsNotFoundException("Post not found"));


            if (!existingComment.getUserId().equals(user.getId())) {
                throw new AccessDeniedException("You can only update your own comment");
            }

            boolean canAccess =
                    post.getType() == TypeOfAccess.PUBLIC ||
                            post.getUserId().equals(user.getId());

            if (!canAccess) {
                throw new AccessDeniedException("Post not accessible");
            }

            if (commentDto.getText() != null && !commentDto.getText().isBlank()) {
                existingComment.setText(commentDto.getText());
            }
            existingComment.setUpdatedAt(LocalDateTime.now());
            return commentRepo.save(existingComment);
        } catch (Exception e) {
            log.error("Failed to update comment {} by user {}", id, userEmail, e);
            throw e;
        }
    }

    private String commentCountKey(ObjectId postId) {
        return "comment:count:" + postId.toHexString();
    }

    public long getCommentCount(ObjectId postId) {
        String key = commentCountKey(postId);
        Long cached = redisService.getLong(key);
        if (cached != null) {
            log.info("CACHE HIT");
            return cached;
        }
        log.info("CACHE MISS");
        long dbCount = commentRepo.countByPostId(postId);
        redisService.setLong(key, dbCount, null);
        return dbCount;
    }
}

