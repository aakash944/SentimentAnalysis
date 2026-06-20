package com.example.demo.sentiment_analysis.comment.service;

import com.example.demo.sentiment_analysis.comment.comment_request.CommentRequest;
import com.example.demo.sentiment_analysis.comment.comment_response.CommentResponseDto;
import com.example.demo.sentiment_analysis.comment.model.Comment;
import com.example.demo.sentiment_analysis.comment.repository.CommentRepo;
import com.example.demo.sentiment_analysis.exception.CommentNotFoundException;
import com.example.demo.sentiment_analysis.exception.PostsNotFoundException;
import com.example.demo.sentiment_analysis.exception.UserNotFoundException;
import com.example.demo.sentiment_analysis.exception.GibberishCommentException;
import com.example.demo.sentiment_analysis.posts.enumeration.TypeOfAccess;
import com.example.demo.sentiment_analysis.posts.model.Posts;
import com.example.demo.sentiment_analysis.posts.repository.PostsRepo;
import com.example.demo.sentiment_analysis.realtime_websocket.service.PostRealtimePublisher;
import com.example.demo.sentiment_analysis.redis.service.RedisService;
import com.example.demo.sentiment_analysis.slice_response_dto.PaginatedResponse;
import com.example.demo.sentiment_analysis.user.model.Users;
import com.example.demo.sentiment_analysis.user.repository.UserRepo;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentServiceTest {

    @Mock private CommentRepo commentRepo;
    @Mock private UserRepo userRepo;
    @Mock private PostsRepo postsRepo;
    @Mock private PostRealtimePublisher postRealtimePublisher;
    @Mock private RedisService redisService;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private CommentService commentService;

    private ObjectId userId;
    private ObjectId postId;
    private ObjectId commentId;
    private Users user;
    private Posts publicPost;
    private Posts privatePost;
    private Comment comment;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        userId    = new ObjectId();
        postId    = new ObjectId();
        commentId = new ObjectId();
        pageable  = PageRequest.of(0, 10);

        user = new Users();
        user.setId(userId);
        user.setUserEmail("user@test.com");

        publicPost = new Posts();
        publicPost.setId(postId);
        publicPost.setUserId(new ObjectId()); // different owner
        publicPost.setType(TypeOfAccess.PUBLIC);

        privatePost = new Posts();
        privatePost.setId(postId);
        privatePost.setUserId(new ObjectId()); // different owner
        privatePost.setType(TypeOfAccess.PRIVATE);

        comment = new Comment();
        comment.setId(commentId);
        comment.setUserId(userId);
        comment.setPostId(postId);
        comment.setText("Hello world");
        comment.setUpdatedAt(LocalDateTime.now());
    }

    // =========================================================================
    // getCommentByEmail
    // =========================================================================

    @Test
    @DisplayName("getCommentByEmail: user not found → throws UserNotFoundException")
    void getCommentByEmail_userNotFound_throws() {
        when(userRepo.findByUserEmail("x@test.com")).thenReturn(null);

        assertThatThrownBy(() -> commentService.getCommentByEmail("x@test.com", pageable))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("getCommentByEmail: success → returns paginated comments")
    void getCommentByEmail_success_returnsPaginatedResponse() {
        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(postsRepo.findAll()).thenReturn(List.of(publicPost));
        when(commentRepo.findByPostIdIn(anyList(), eq(pageable)))
                .thenReturn(new SliceImpl<>(List.of(comment)));
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));

        PaginatedResponse<CommentResponseDto> result =
                commentService.getCommentByEmail("user@test.com", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getText()).isEqualTo("Hello world");
    }

    @Test
    @DisplayName("getCommentByEmail: comment user not found → shows 'unknown'")
    void getCommentByEmail_commentUserUnknown_usesUnknown() {
        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(postsRepo.findAll()).thenReturn(List.of(publicPost));
        when(commentRepo.findByPostIdIn(anyList(), eq(pageable)))
                .thenReturn(new SliceImpl<>(List.of(comment)));
        when(userRepo.findById(any(ObjectId.class))).thenReturn(Optional.empty());

        PaginatedResponse<CommentResponseDto> result =
                commentService.getCommentByEmail("user@test.com", pageable);

        assertThat(result.getContent().get(0).getUserEmail()).isEqualTo("unknown");
    }

    // =========================================================================
    // newComment
    // =========================================================================

    @Test
    @DisplayName("newComment: user not found → throws UserNotFoundException")
    void newComment_userNotFound_throws() {
        when(userRepo.findByUserEmail("x@test.com")).thenReturn(null);

        CommentRequest req = new CommentRequest(postId, "Good post");
        assertThatThrownBy(() -> commentService.newComment(req, "x@test.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("newComment: post not found → throws PostsNotFoundException")
    void newComment_postNotFound_throws() {
        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(postsRepo.findById(postId)).thenReturn(Optional.empty());

        CommentRequest req = new CommentRequest(postId, "Good post");
        assertThatThrownBy(() -> commentService.newComment(req, "user@test.com"))
                .isInstanceOf(PostsNotFoundException.class);
    }

    @Test
    @DisplayName("newComment: private post not owned → throws AccessDeniedException")
    void newComment_privatePostNotOwned_throws() {
        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(postsRepo.findById(postId)).thenReturn(Optional.of(privatePost));

        CommentRequest req = new CommentRequest(postId, "Good post");
        assertThatThrownBy(() -> commentService.newComment(req, "user@test.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("cannot comment");
    }

    @Test
    @DisplayName("newComment: text too short → throws GibberishCommentException")
    void newComment_textTooShort_throws() {
        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(postsRepo.findById(postId)).thenReturn(Optional.of(publicPost));

        CommentRequest req = new CommentRequest(postId, "Hi"); // < 3 chars
        assertThatThrownBy(() -> commentService.newComment(req, "user@test.com"))
                .isInstanceOf(GibberishCommentException.class)
                .hasMessage("Comment too short");
    }

    @Test
    @DisplayName("newComment: empty text → throws GibberishCommentException")
    void newComment_emptyText_throws() {
        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(postsRepo.findById(postId)).thenReturn(Optional.of(publicPost));

        CommentRequest req = new CommentRequest(postId, "   ");
        assertThatThrownBy(() -> commentService.newComment(req, "user@test.com"))
                .isInstanceOf(GibberishCommentException.class);
    }

    @Test
    @DisplayName("newComment: valid request → saves comment, sends Kafka, increments Redis, publishes event")
    void newComment_valid_savesAndPublishes() throws AccessDeniedException {
        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(postsRepo.findById(postId)).thenReturn(Optional.of(publicPost));
        when(commentRepo.save(any(Comment.class))).thenReturn(comment);

        CommentRequest req = new CommentRequest(postId, "Great post!");
        CommentResponseDto result = commentService.newComment(req, "user@test.com");

        verify(commentRepo).save(any(Comment.class));
        verify(kafkaTemplate).send(eq("comments-topic"), anyString(), anyString());
        verify(redisService).increment("comment:count:" + postId.toHexString());
        verify(postRealtimePublisher).publishPostUpdate(postId, "COMMENT_CREATED", "user@test.com");
        assertThat(result).isNotNull();
    }

    // =========================================================================
    // removeComment
    // =========================================================================

    @Test
    @DisplayName("removeComment: user not found → logs error silently (no rethrow)")
    void removeComment_userNotFound_handlesGracefully() throws AccessDeniedException {
        when(userRepo.findByUserEmail("x@test.com")).thenReturn(null);

        // removeComment catches all exceptions internally — should not throw
        commentService.removeComment(commentId, "x@test.com");

        verify(commentRepo, never()).delete(any());
    }

    @Test
    @DisplayName("removeComment: comment not found → handled gracefully")
    void removeComment_commentNotFound_handlesGracefully() throws AccessDeniedException {
        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(commentRepo.findById(commentId)).thenReturn(Optional.empty());

        commentService.removeComment(commentId, "user@test.com");

        verify(commentRepo, never()).delete(any());
    }

    @Test
    @DisplayName("removeComment: not allowed (not post owner or comment owner) → handled gracefully")
    void removeComment_notAllowed_handlesGracefully() throws AccessDeniedException {
        ObjectId otherUserId = new ObjectId();
        comment.setUserId(otherUserId); // comment owned by different user

        Posts ownedByOther = new Posts();
        ownedByOther.setId(postId);
        ownedByOther.setUserId(otherUserId); // post owned by other user too

        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(commentRepo.findById(commentId)).thenReturn(Optional.of(comment));
        when(postsRepo.findById(postId)).thenReturn(Optional.of(ownedByOther));

        commentService.removeComment(commentId, "user@test.com");

        verify(commentRepo, never()).delete(any());
    }

    @Test
    @DisplayName("removeComment: success → deletes comment, decrements Redis, publishes COMMENT_DELETED")
    void removeComment_success_deletesAndPublishes() throws AccessDeniedException {
        comment.setUserId(userId); // user owns the comment

        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(commentRepo.findById(commentId)).thenReturn(Optional.of(comment));
        when(postsRepo.findById(postId)).thenReturn(Optional.of(publicPost));

        commentService.removeComment(commentId, "user@test.com");

        verify(commentRepo).delete(comment);
        verify(redisService).decrement("comment:count:" + postId.toHexString());
        verify(postRealtimePublisher).publishPostUpdate(postId, "COMMENT_DELETED", "user@test.com");
    }

    // =========================================================================
    // updateComment
    // =========================================================================

    @Test
    @DisplayName("updateComment: user not found → throws UsernameNotFoundException")
    void updateComment_userNotFound_throws() {
        when(userRepo.findByUserEmail("x@test.com")).thenReturn(null);

        CommentRequest req = new CommentRequest(postId, "Updated text");
        assertThatThrownBy(() -> commentService.updateComment(commentId, req, "x@test.com"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("updateComment: comment not found → throws CommentNotFoundException")
    void updateComment_commentNotFound_throws() {
        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(commentRepo.findById(commentId)).thenReturn(Optional.empty());

        CommentRequest req = new CommentRequest(postId, "Updated text");
        assertThatThrownBy(() -> commentService.updateComment(commentId, req, "user@test.com"))
                .isInstanceOf(CommentNotFoundException.class);
    }

    @Test
    @DisplayName("updateComment: not comment owner → throws AccessDeniedException")
    void updateComment_notCommentOwner_throws() {
        comment.setUserId(new ObjectId()); // different owner

        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(commentRepo.findById(commentId)).thenReturn(Optional.of(comment));
        when(postsRepo.findById(postId)).thenReturn(Optional.of(publicPost));

        CommentRequest req = new CommentRequest(postId, "Updated text");
        assertThatThrownBy(() -> commentService.updateComment(commentId, req, "user@test.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("own comment");
    }

    @Test
    @DisplayName("updateComment: post not accessible (private, not owner) → throws AccessDeniedException")
    void updateComment_postNotAccessible_throws() {
        comment.setUserId(userId); // user owns comment

        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(commentRepo.findById(commentId)).thenReturn(Optional.of(comment));
        when(postsRepo.findById(postId)).thenReturn(Optional.of(privatePost)); // private, different owner

        CommentRequest req = new CommentRequest(postId, "Updated text");
        assertThatThrownBy(() -> commentService.updateComment(commentId, req, "user@test.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Post not accessible");
    }

    @Test
    @DisplayName("updateComment: success → updates text and saves")
    void updateComment_success_updatesAndSaves() throws AccessDeniedException {
        comment.setUserId(userId); // user owns comment

        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(commentRepo.findById(commentId)).thenReturn(Optional.of(comment));
        when(postsRepo.findById(postId)).thenReturn(Optional.of(publicPost));
        when(commentRepo.save(comment)).thenReturn(comment);

        CommentRequest req = new CommentRequest(postId, "Updated text");
        Comment result = commentService.updateComment(commentId, req, "user@test.com");

        verify(commentRepo).save(comment);
        assertThat(result.getText()).isEqualTo("Updated text");
    }

    @Test
    @DisplayName("updateComment: null text in request → text unchanged, updatedAt refreshed")
    void updateComment_nullText_textUnchanged() throws AccessDeniedException {
        comment.setUserId(userId);
        comment.setText("Original text");

        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(commentRepo.findById(commentId)).thenReturn(Optional.of(comment));
        when(postsRepo.findById(postId)).thenReturn(Optional.of(publicPost));
        when(commentRepo.save(comment)).thenReturn(comment);

        CommentRequest req = new CommentRequest(postId, null);
        Comment result = commentService.updateComment(commentId, req, "user@test.com");

        assertThat(result.getText()).isEqualTo("Original text");
        verify(commentRepo).save(comment);
    }

    // =========================================================================
    // getCommentCount
    // =========================================================================

    @Test
    @DisplayName("getCommentCount: cache hit → returns cached value, no DB call")
    void getCommentCount_cacheHit_returnsCached() {
        when(redisService.getLong("comment:count:" + postId.toHexString())).thenReturn(7L);

        long count = commentService.getCommentCount(postId);

        assertThat(count).isEqualTo(7L);
        verifyNoInteractions(commentRepo);
    }

    @Test
    @DisplayName("getCommentCount: cache miss → queries DB, stores in Redis")
    void getCommentCount_cacheMiss_queriesDbAndStores() {
        when(redisService.getLong("comment:count:" + postId.toHexString())).thenReturn(null);
        when(commentRepo.countByPostId(postId)).thenReturn(4L);

        long count = commentService.getCommentCount(postId);

        assertThat(count).isEqualTo(4L);
        verify(commentRepo).countByPostId(postId);
        verify(redisService).setLong("comment:count:" + postId.toHexString(), 4L, null);
    }
}
