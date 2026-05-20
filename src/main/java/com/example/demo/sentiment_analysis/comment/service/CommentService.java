package com.example.demo.sentiment_analysis.comment.service;

import com.example.demo.sentiment_analysis.dto.CommentDto;
import com.example.demo.sentiment_analysis.enumeration.TypeOfAccess;
import com.example.demo.sentiment_analysis.exception.CommentNotFoundException;
import com.example.demo.sentiment_analysis.exception.PostsNotFoundException;
import com.example.demo.sentiment_analysis.comment.repository.CommentRepo;
import com.example.demo.sentiment_analysis.comment.model.Comment;
import com.example.demo.sentiment_analysis.posts.model.Posts;
import com.example.demo.sentiment_analysis.user.model.Users;
import com.example.demo.sentiment_analysis.response_dto.comment_response.CommentResponseDto;
import com.example.demo.sentiment_analysis.response_dto.PaginatedResponse;
import com.example.demo.sentiment_analysis.posts.repository.PostsRepo;
import com.example.demo.sentiment_analysis.user.repository.UserRepo;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;


@Service
public class CommentService {
    private final CommentRepo commentRepo;
    private final UserRepo userRepo;
    private final PostsRepo postsRepo;

    public CommentService(CommentRepo commentRepo, UserRepo userRepo, PostsRepo postsRepo) {
        this.commentRepo = commentRepo;

        this.userRepo = userRepo;
        this.postsRepo = postsRepo;
    }

    public PaginatedResponse<CommentResponseDto> getCommentByEmail(String userEmail, Pageable pageable) {

        Users user = userRepo.findByUserEmail(userEmail);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
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
                            comment.getPostId().toHexString(),
                            commentUser != null ? commentUser.getUserEmail() : "unknown",
                            comment.getText(),
                            comment.getCreatedAt()
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

    public Comment newComment(CommentDto commentDto, String userEmail) throws AccessDeniedException {

        Users currentUser = userRepo.findByUserEmail(userEmail);

        if (currentUser == null) {
            throw new UsernameNotFoundException("User not found");
        }

        Posts post = postsRepo.findById(commentDto.getPostId())
                .orElseThrow(() -> new PostsNotFoundException("Post not found exception"));


        boolean canAccess =
                post.getType() == TypeOfAccess.PUBLIC ||
                        post.getUserId().equals(currentUser.getId());

        if (!canAccess) {
            throw new AccessDeniedException("You cannot comment on this post");
        }

        Comment comment = new Comment();
        comment.setUserId(currentUser.getId());
        comment.setPostId(post.getId());
        comment.setText(commentDto.getText());
        comment.setCreatedAt(LocalDateTime.now());

        return commentRepo.save(comment);
    }

    public void removeComment(ObjectId id, String userEmail) throws AccessDeniedException {

        Users user = userRepo.findByUserEmail(userEmail);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        Comment comment = commentRepo.findById(id)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));

        Posts post = postsRepo.findById(comment.getPostId())
                .orElseThrow(() -> new PostsNotFoundException("Post not found"));

        boolean canDelete =
                        post.getUserId().equals(user.getId()) ||
                        comment.getUserId().equals(user.getId());

        if (!canDelete) {
            throw new AccessDeniedException("Not allowed to delete this comment");
        }

        commentRepo.delete(comment);
    }

    public Comment updateComment(ObjectId id, CommentDto commentDto, String userEmail) throws AccessDeniedException {

        Users user = userRepo.findByUserEmail(userEmail);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        Comment existingComment = commentRepo.findById(id)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));

        Posts post = postsRepo.findById(existingComment.getPostId())
                .orElseThrow(() -> new PostsNotFoundException("Post not found"));

        // ownership check
        if (!existingComment.getUserId().equals(user.getId())) {
            throw new AccessDeniedException("You can only update your own comment");
        }

        // optional: check post visibility
        boolean canAccess =
                post.getType() == TypeOfAccess.PUBLIC ||
                        post.getUserId().equals(user.getId());

        if (!canAccess) {
            throw new AccessDeniedException("Post not accessible");
        }

        // UPDATE (NOT CREATE)
        if (commentDto.getText() != null && !commentDto.getText().isBlank()) {
            existingComment.setText(commentDto.getText());
        }

        existingComment.setCreatedAt(LocalDateTime.now());

        return commentRepo.save(existingComment);
    }
}

