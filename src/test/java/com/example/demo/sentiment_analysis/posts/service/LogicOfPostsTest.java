package com.example.demo.sentiment_analysis.posts.service;

import com.example.demo.sentiment_analysis.ai.enumeration.SentimentType;
import com.example.demo.sentiment_analysis.comment.model.Comment;
import com.example.demo.sentiment_analysis.comment.repository.CommentRepo;
import com.example.demo.sentiment_analysis.exception.PostsNotFoundException;
import com.example.demo.sentiment_analysis.exception.UserNotFoundException;
import com.example.demo.sentiment_analysis.posts.enumeration.TypeOfAccess;
import com.example.demo.sentiment_analysis.posts.model.Posts;
import com.example.demo.sentiment_analysis.posts.post_request_dto.PostDto;
import com.example.demo.sentiment_analysis.posts.posts_response.PostDetailDto;
import com.example.demo.sentiment_analysis.posts.posts_response.PostResponseDto;
import com.example.demo.sentiment_analysis.posts.repository.PostsRepo;
import com.example.demo.sentiment_analysis.reaction.enumeration.ReactionType;
import com.example.demo.sentiment_analysis.reaction.model.Reaction;
import com.example.demo.sentiment_analysis.reaction.repository.ReactionRepo;
import com.example.demo.sentiment_analysis.redis.service.RedisService;
import com.example.demo.sentiment_analysis.slice_response_dto.PaginatedResponse;
import com.example.demo.sentiment_analysis.user.model.Users;
import com.example.demo.sentiment_analysis.user.repository.UserRepo;
import com.fasterxml.jackson.core.type.TypeReference;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogicOfPostsTest {

    @Mock
    private PostsRepo postsRepo;

    @Mock
    private UserRepo userRepo;

    @Mock
    private CommentRepo commentRepo;

    @Mock
    private ReactionRepo reactionRepo;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private LogicOfPosts logicOfPosts;

    @Test
    void getPostsByUserEmailReturnsCachedResponseWhenPresent() {
        Pageable pageable = PageRequest.of(0, 5);
        PaginatedResponse<PostResponseDto> cachedResponse = new PaginatedResponse<>();
        cachedResponse.setContent(List.of(new PostResponseDto(
                new ObjectId().toHexString(),
                "Cached title",
                "Cached content",
                "author@example.com",
                LocalDateTime.now(),
                2L,
                3L
        )));

        when(redisService.get(eq("feed:viewer@example.com:page:0:size:5"), any(TypeReference.class)))
                .thenReturn(cachedResponse);

        PaginatedResponse<PostResponseDto> result = logicOfPosts.getPostsByUserEmail("viewer@example.com", pageable);

        assertSame(cachedResponse, result);
        verifyNoInteractions(userRepo, postsRepo, commentRepo, reactionRepo);
        verify(redisService, never()).set(any(), any(), any());
    }

    @Test
    void getPostsByUserEmailReturnsPostsFromRepositoryAndCachesResponse() {
        Pageable pageable = PageRequest.of(0, 5);
        ObjectId userId = new ObjectId();
        ObjectId postId = new ObjectId();
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 17, 9, 15);
        Users currentUser = user(userId, "viewer@example.com");
        Posts post = new Posts(postId, userId, "Feed title", "Feed content", TypeOfAccess.PUBLIC, createdAt);

        when(redisService.get(eq("feed:viewer@example.com:page:0:size:5"), any(TypeReference.class)))
                .thenReturn(null);
        when(userRepo.findByUserEmail("viewer@example.com")).thenReturn(currentUser);
        when(postsRepo.findByTypeOrUserId(TypeOfAccess.PUBLIC, userId, pageable))
                .thenReturn(new SliceImpl<>(List.of(post), pageable, false));
        when(userRepo.findById(userId)).thenReturn(Optional.of(currentUser));
        when(commentRepo.countByPostId(postId)).thenReturn(4L);
        when(reactionRepo.countByPostId(postId)).thenReturn(5L);

        PaginatedResponse<PostResponseDto> result = logicOfPosts.getPostsByUserEmail("viewer@example.com", pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(postId.toHexString(), result.getContent().get(0).getPostId());
        assertEquals("Feed title", result.getContent().get(0).getTitle());
        assertEquals("Feed content", result.getContent().get(0).getText());
        assertEquals("viewer@example.com", result.getContent().get(0).getUserEmail());
        assertEquals(4L, result.getContent().get(0).getCommentCount());
        assertEquals(5L, result.getContent().get(0).getReactionCount());
        assertEquals(0, result.getPageNumber());
        assertEquals(5, result.getPageSize());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());

        verify(redisService).set("feed:viewer@example.com:page:0:size:5", result, 30L);
    }

    @Test
    void getPostsByUserEmailThrowsUsernameNotFoundExceptionWhenUserDoesNotExist() {
        Pageable pageable = PageRequest.of(0, 5);

        when(redisService.get(eq("feed:missing@example.com:page:0:size:5"), any(TypeReference.class)))
                .thenReturn(null);
        when(userRepo.findByUserEmail("missing@example.com")).thenReturn(null);

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> logicOfPosts.getPostsByUserEmail("missing@example.com", pageable)
        );

        assertEquals("User not found", exception.getMessage());
        verifyNoInteractions(postsRepo, commentRepo, reactionRepo);
        verify(redisService, never()).set(any(), any(), any());
    }

    @Test
    void createPostForUserSavesPostWithCurrentUserAndRequestData() {
        ObjectId userId = new ObjectId();
        Users currentUser = user(userId, "author@example.com");
        PostDto postDto = new PostDto(null, "New title", "New content", TypeOfAccess.PRIVATE);
        Posts savedPost = new Posts(new ObjectId(), userId, "New title", "New content", TypeOfAccess.PRIVATE, LocalDateTime.now());

        when(userRepo.findByUserEmail("author@example.com")).thenReturn(currentUser);
        when(postsRepo.save(any(Posts.class))).thenReturn(savedPost);

        Posts result = logicOfPosts.createPostForUser(postDto, "author@example.com");

        assertSame(savedPost, result);
        ArgumentCaptor<Posts> postsCaptor = ArgumentCaptor.forClass(Posts.class);
        verify(postsRepo).save(postsCaptor.capture());
        Posts postToSave = postsCaptor.getValue();
        assertEquals(userId, postToSave.getUserId());
        assertEquals("New title", postToSave.getTitle());
        assertEquals("New content", postToSave.getContent());
        assertEquals(TypeOfAccess.PRIVATE, postToSave.getType());
        assertNotNull(postToSave.getCreateAt());
    }

    @Test
    void removePostDeletesCommentsReactionsAndPostWhenCurrentUserOwnsPost() throws AccessDeniedException {
        ObjectId postId = new ObjectId();
        ObjectId userId = new ObjectId();
        Users currentUser = user(userId, "author@example.com");
        Posts post = new Posts(postId, userId, "Title", "Content", TypeOfAccess.PUBLIC, LocalDateTime.now());

        when(userRepo.findByUserEmail("author@example.com")).thenReturn(currentUser);
        when(postsRepo.findById(postId)).thenReturn(Optional.of(post));

        logicOfPosts.removePost(postId, "author@example.com");

        verify(commentRepo).deleteByPostId(postId);
        verify(reactionRepo).deleteByPostId(postId);
        verify(postsRepo).delete(post);
    }

    @Test
    void removePostThrowsPostsNotFoundExceptionWhenPostDoesNotExist() {
        ObjectId postId = new ObjectId();

        when(userRepo.findByUserEmail("author@example.com")).thenReturn(user(new ObjectId(), "author@example.com"));
        when(postsRepo.findById(postId)).thenReturn(Optional.empty());

        PostsNotFoundException exception = assertThrows(
                PostsNotFoundException.class,
                () -> logicOfPosts.removePost(postId, "author@example.com")
        );

        assertEquals("Post not found", exception.getMessage());
        verifyNoInteractions(commentRepo, reactionRepo);
        verify(postsRepo, never()).delete(any());
    }

    @Test
    void removePostThrowsAccessDeniedExceptionWhenCurrentUserDoesNotOwnPost() {
        ObjectId postId = new ObjectId();
        Users currentUser = user(new ObjectId(), "viewer@example.com");
        Posts post = new Posts(postId, new ObjectId(), "Title", "Content", TypeOfAccess.PUBLIC, LocalDateTime.now());

        when(userRepo.findByUserEmail("viewer@example.com")).thenReturn(currentUser);
        when(postsRepo.findById(postId)).thenReturn(Optional.of(post));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> logicOfPosts.removePost(postId, "viewer@example.com")
        );

        assertEquals("You can delete only your own post", exception.getMessage());
        verifyNoInteractions(commentRepo, reactionRepo);
        verify(postsRepo, never()).delete(any());
    }

    @Test
    void updatePostUpdatesContentTitleAndSavesWhenCurrentUserOwnsPost() {
        ObjectId postId = new ObjectId();
        ObjectId userId = new ObjectId();
        Posts existingPost = new Posts(postId, userId, "Old title", "Old content", TypeOfAccess.PUBLIC, LocalDateTime.now().minusDays(1));
        PostDto postDto = new PostDto(postId, "Updated title", "Updated content", TypeOfAccess.PUBLIC);

        when(userRepo.findByUserEmail("author@example.com")).thenReturn(user(userId, "author@example.com"));
        when(postsRepo.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postsRepo.save(existingPost)).thenReturn(existingPost);

        Posts result = logicOfPosts.updatePost(postId, postDto, "author@example.com");

        assertSame(existingPost, result);
        assertEquals("Updated title", result.getTitle());
        assertEquals("Updated content", result.getContent());
        verify(postsRepo).save(existingPost);
    }

    @Test
    void updatePostThrowsPostsNotFoundExceptionWhenPostDoesNotExist() {
        ObjectId postId = new ObjectId();
        PostDto postDto = new PostDto(postId, "Updated title", "Updated content", TypeOfAccess.PUBLIC);

        when(userRepo.findByUserEmail("author@example.com")).thenReturn(user(new ObjectId(), "author@example.com"));
        when(postsRepo.findById(postId)).thenReturn(Optional.empty());

        PostsNotFoundException exception = assertThrows(
                PostsNotFoundException.class,
                () -> logicOfPosts.updatePost(postId, postDto, "author@example.com")
        );

        assertEquals("Post is not found", exception.getMessage());
        verify(postsRepo, never()).save(any());
    }

    @Test
    void updatePostThrowsUserNotFoundExceptionWhenCurrentUserDoesNotOwnPost() {
        ObjectId postId = new ObjectId();
        PostDto postDto = new PostDto(postId, "Updated title", "Updated content", TypeOfAccess.PUBLIC);

        when(userRepo.findByUserEmail("viewer@example.com")).thenReturn(user(new ObjectId(), "viewer@example.com"));
        when(postsRepo.findById(postId)).thenReturn(Optional.of(
                new Posts(postId, new ObjectId(), "Old title", "Old content", TypeOfAccess.PUBLIC, LocalDateTime.now())
        ));

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> logicOfPosts.updatePost(postId, postDto, "viewer@example.com")
        );

        assertEquals("User not found exception", exception.getMessage());
        verify(postsRepo, never()).save(any());
    }

    @Test
    void getPostDetailReturnsPostCommentsAndReactionsWhenPublicPostExists() throws AccessDeniedException {
        ObjectId postId = new ObjectId();
        ObjectId currentUserId = new ObjectId();
        ObjectId postOwnerId = new ObjectId();
        ObjectId commentUserId = new ObjectId();
        ObjectId reactionUserId = new ObjectId();
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 17, 10, 30);
        LocalDateTime commentUpdatedAt = LocalDateTime.of(2026, 6, 17, 10, 45);
        LocalDateTime reactionCreatedAt = LocalDateTime.of(2026, 6, 17, 11, 0);

        Users currentUser = new Users(currentUserId, "viewer@example.com", "password", createdAt, List.of("USER"));
        Users postOwner = new Users(postOwnerId, "author@example.com", "password", createdAt, List.of("USER"));
        Users commentUser = new Users(commentUserId, "commenter@example.com", "password", createdAt, List.of("USER"));
        Users reactionUser = new Users(reactionUserId, "reactor@example.com", "password", createdAt, List.of("USER"));
        Posts post = new Posts(postId, postOwnerId, "Test title", "Test content", TypeOfAccess.PUBLIC, createdAt);
        Comment comment = new Comment(new ObjectId(), commentUserId, postId, "Nice post", SentimentType.POSITIVE, 0.92, commentUpdatedAt);
        Reaction reaction = new Reaction(new ObjectId(), reactionUserId, postId, ReactionType.LIKE, reactionCreatedAt);

        when(userRepo.findByUserEmail("viewer@example.com")).thenReturn(currentUser);
        when(postsRepo.findById(postId)).thenReturn(Optional.of(post));
        when(userRepo.findById(postOwnerId)).thenReturn(Optional.of(postOwner));
        when(commentRepo.countByPostId(postId)).thenReturn(1L);
        when(reactionRepo.countByPostId(postId)).thenReturn(1L);
        when(commentRepo.findByPostId(postId)).thenReturn(List.of(comment));
        when(userRepo.findById(commentUserId)).thenReturn(Optional.of(commentUser));
        when(reactionRepo.findByPostId(postId)).thenReturn(List.of(reaction));
        when(userRepo.findById(reactionUserId)).thenReturn(Optional.of(reactionUser));

        PostDetailDto result = logicOfPosts.getPostDetail(postId, "viewer@example.com");

        assertEquals(postId.toHexString(), result.getPost().getPostId());
        assertEquals("Test title", result.getPost().getTitle());
        assertEquals("Test content", result.getPost().getText());
        assertEquals("author@example.com", result.getPost().getUserEmail());
        assertEquals(1L, result.getPost().getCommentCount());
        assertEquals(1L, result.getPost().getReactionCount());
        assertEquals("Nice post", result.getComments().get(0).getText());
        assertEquals("commenter@example.com", result.getComments().get(0).getUserEmail());
        assertEquals("LIKE", result.getReactions().get(0).getReactionType());
        assertEquals("reactor@example.com", result.getReactions().get(0).getUserEmail());
    }

    @Test
    void getPostDetailThrowsPostsNotFoundExceptionWhenPostDoesNotExist() {
        ObjectId postId = new ObjectId();
        Users currentUser = new Users(new ObjectId(), "viewer@example.com", "password", LocalDateTime.now(), List.of("USER"));

        when(userRepo.findByUserEmail("viewer@example.com")).thenReturn(currentUser);
        when(postsRepo.findById(postId)).thenReturn(Optional.empty());

        PostsNotFoundException exception = assertThrows(
                PostsNotFoundException.class,
                () -> logicOfPosts.getPostDetail(postId, "viewer@example.com")
        );

        assertEquals("Post not found", exception.getMessage());
        verify(postsRepo).findById(postId);
        verifyNoInteractions(commentRepo, reactionRepo);
    }

    @Test
    void getPostDetailThrowsAccessDeniedExceptionWhenPrivatePostBelongsToAnotherUser() {
        ObjectId postId = new ObjectId();
        ObjectId currentUserId = new ObjectId();
        ObjectId postOwnerId = new ObjectId();
        Users currentUser = new Users(currentUserId, "viewer@example.com", "password", LocalDateTime.now(), List.of("USER"));
        Posts privatePost = new Posts(postId, postOwnerId, "Private title", "Private content", TypeOfAccess.PRIVATE, LocalDateTime.now());

        when(userRepo.findByUserEmail("viewer@example.com")).thenReturn(currentUser);
        when(postsRepo.findById(postId)).thenReturn(Optional.of(privatePost));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> logicOfPosts.getPostDetail(postId, "viewer@example.com")
        );

        assertEquals("You cannot view this post", exception.getMessage());
        verifyNoInteractions(commentRepo, reactionRepo);
    }

    @Test
    void getPostDetailThrowsUserNotFoundExceptionWhenCurrentUserDoesNotExist() {
        ObjectId postId = new ObjectId();

        when(userRepo.findByUserEmail("missing@example.com")).thenReturn(null);

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> logicOfPosts.getPostDetail(postId, "missing@example.com")
        );

        assertEquals("User not found", exception.getMessage());
        verifyNoInteractions(postsRepo, commentRepo, reactionRepo);
    }

    private Users user(ObjectId userId, String email) {
        return new Users(userId, email, "password", LocalDateTime.now(), List.of("USER"));
    }
}
