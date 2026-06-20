package com.example.demo.sentiment_analysis.reaction.service;

import com.example.demo.sentiment_analysis.exception.PostsNotFoundException;
import com.example.demo.sentiment_analysis.exception.UserNotFoundException;
import com.example.demo.sentiment_analysis.posts.enumeration.TypeOfAccess;
import com.example.demo.sentiment_analysis.posts.model.Posts;
import com.example.demo.sentiment_analysis.posts.repository.PostsRepo;
import com.example.demo.sentiment_analysis.reaction.dto.ReactionDto;
import com.example.demo.sentiment_analysis.reaction.enumeration.ReactionType;
import com.example.demo.sentiment_analysis.reaction.model.Reaction;
import com.example.demo.sentiment_analysis.reaction.repository.ReactionRepo;
import com.example.demo.sentiment_analysis.reaction.reaction_response.ReactionResponseDto;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReactionServiceTest {

    @Mock private ReactionRepo reactionRepo;
    @Mock private UserRepo userRepo;
    @Mock private PostsRepo postsRepo;
    @Mock private PostRealtimePublisher postRealtimePublisher;
    @Mock private RedisService redisService;

    @InjectMocks
    private ReactionService reactionService;

    private ObjectId userId;
    private ObjectId postId;
    private Users user;
    private Posts publicPost;
    private Posts privatePost;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        userId  = new ObjectId();
        postId  = new ObjectId();
        pageable = PageRequest.of(0, 10);

        user = new Users();
        user.setId(userId);
        user.setUserEmail("user@test.com");

        publicPost = new Posts();
        publicPost.setId(postId);
        publicPost.setUserId(new ObjectId()); // different owner
        publicPost.setType(TypeOfAccess.PUBLIC);

        privatePost = new Posts();
        privatePost.setId(postId);
        privatePost.setUserId(new ObjectId()); // different owner → not accessible
        privatePost.setType(TypeOfAccess.PRIVATE);
    }

    @Test
    @DisplayName("getAllReactions: user not found → throws UsernameNotFoundException")
    void getAllReactions_userNotFound_throws() {
        when(userRepo.findByUserEmail("unknown@test.com")).thenReturn(null);

        assertThatThrownBy(() -> reactionService.getAllReactions("unknown@test.com", pageable))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("getAllReactions: returns paginated reactions for visible posts")
    void getAllReactions_success_returnsPaginatedResponse() {
        Reaction reaction = new Reaction();
        reaction.setId(new ObjectId());
        reaction.setUserId(userId);
        reaction.setPostId(postId);
        reaction.setReactionType(ReactionType.LIKE);
        reaction.setCreatedAt(LocalDateTime.now());

        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(postsRepo.findAll()).thenReturn(List.of(publicPost));
        when(reactionRepo.findByPostIdIn(anyList(), eq(pageable)))
                .thenReturn(new SliceImpl<>(List.of(reaction)));
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));

        PaginatedResponse<ReactionResponseDto> result =
                reactionService.getAllReactions("user@test.com", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserEmail()).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("getAllReactions: reaction user not found → email shown as 'unknown'")
    void getAllReactions_reactionUserNotFound_usesUnknown() {
        Reaction reaction = new Reaction();
        reaction.setId(new ObjectId());
        reaction.setUserId(new ObjectId()); // unknown user
        reaction.setPostId(postId);
        reaction.setReactionType(ReactionType.LIKE);
        reaction.setCreatedAt(LocalDateTime.now());

        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(postsRepo.findAll()).thenReturn(List.of(publicPost));
        when(reactionRepo.findByPostIdIn(anyList(), eq(pageable)))
                .thenReturn(new SliceImpl<>(List.of(reaction)));
        when(userRepo.findById(any(ObjectId.class))).thenReturn(Optional.empty());

        PaginatedResponse<ReactionResponseDto> result =
                reactionService.getAllReactions("user@test.com", pageable);

        assertThat(result.getContent().get(0).getUserEmail()).isEqualTo("unknown");
    }

    // =========================================================================
    // createReaction
    // =========================================================================

    @Test
    @DisplayName("createReaction: user not found → throws UsernameNotFoundException")
    void createReaction_userNotFound_throws() {
        when(userRepo.findByUserEmail("unknown@test.com")).thenReturn(null);

        ReactionDto dto = new ReactionDto(postId, ReactionType.LIKE);
        assertThatThrownBy(() -> reactionService.createReaction(dto, "unknown@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("createReaction: post not found → throws PostsNotFoundException")
    void createReaction_postNotFound_throws() {
        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(postsRepo.findById(postId)).thenReturn(Optional.empty());

        ReactionDto dto = new ReactionDto(postId, ReactionType.LIKE);
        assertThatThrownBy(() -> reactionService.createReaction(dto, "user@test.com"))
                .isInstanceOf(PostsNotFoundException.class);
    }

    @Test
    @DisplayName("createReaction: private post not owned by user → throws AccessDeniedException")
    void createReaction_privatePostNotOwned_throws() {
        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(postsRepo.findById(postId)).thenReturn(Optional.of(privatePost));

        ReactionDto dto = new ReactionDto(postId, ReactionType.LIKE);
        assertThatThrownBy(() -> reactionService.createReaction(dto, "user@test.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cannot react on private post");
    }

    @Test
    @DisplayName("createReaction: new reaction → saves, increments Redis, publishes REACTION_CREATED")
    void createReaction_newReaction_savesAndPublishes() throws AccessDeniedException {
        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(postsRepo.findById(postId)).thenReturn(Optional.of(publicPost));
        when(reactionRepo.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());

        ReactionDto dto = new ReactionDto(postId, ReactionType.LIKE);
        reactionService.createReaction(dto, "user@test.com");

        verify(reactionRepo).save(any(Reaction.class));
        verify(redisService).increment("reaction:count:" + postId.toHexString());
        verify(postRealtimePublisher).publishPostUpdate(postId, "REACTION_CREATED", "user@test.com");
    }

    @Test
    @DisplayName("createReaction: same reaction type exists → deletes, decrements Redis, publishes REACTION_DELETED")
    void createReaction_sameTypeExists_deletesAndPublishes() throws AccessDeniedException {
        Reaction existing = new Reaction();
        existing.setId(new ObjectId());
        existing.setUserId(userId);
        existing.setPostId(postId);
        existing.setReactionType(ReactionType.LIKE);

        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(postsRepo.findById(postId)).thenReturn(Optional.of(publicPost));
        when(reactionRepo.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(existing));

        ReactionDto dto = new ReactionDto(postId, ReactionType.LIKE); // same type
        reactionService.createReaction(dto, "user@test.com");

        verify(reactionRepo).delete(existing);
        verify(redisService).decrement("reaction:count:" + postId.toHexString());
        verify(postRealtimePublisher).publishPostUpdate(postId, "REACTION_DELETED", "user@test.com");
        verify(reactionRepo, never()).save(any());
    }

    @Test
    @DisplayName("createReaction: different reaction type exists → updates type, publishes REACTION_UPDATED")
    void createReaction_differentTypeExists_updatesAndPublishes() throws AccessDeniedException {
        Reaction existing = new Reaction();
        existing.setId(new ObjectId());
        existing.setUserId(userId);
        existing.setPostId(postId);
        existing.setReactionType(ReactionType.LIKE);

        when(userRepo.findByUserEmail("user@test.com")).thenReturn(user);
        when(postsRepo.findById(postId)).thenReturn(Optional.of(publicPost));
        when(reactionRepo.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(existing));

        ReactionDto dto = new ReactionDto(postId, ReactionType.LOVE); // different type
        reactionService.createReaction(dto, "user@test.com");

        verify(reactionRepo).save(existing);
        verify(postRealtimePublisher).publishPostUpdate(postId, "REACTION_UPDATED", "user@test.com");
        verify(redisService, never()).decrement(anyString());
    }

    // =========================================================================
    // getReactionCount
    // =========================================================================

    @Test
    @DisplayName("getReactionCount: cache hit → returns cached value, no DB call")
    void getReactionCount_cacheHit_returnsCached() {
        when(redisService.getLong("reaction:count:" + postId.toHexString())).thenReturn(5L);

        long count = reactionService.getReactionCount(postId);

        assertThat(count).isEqualTo(5L);
        verifyNoInteractions(reactionRepo);
    }

    @Test
    @DisplayName("getReactionCount: cache miss → queries DB, stores in Redis")
    void getReactionCount_cacheMiss_queriesDbAndStores() {
        when(redisService.getLong("reaction:count:" + postId.toHexString())).thenReturn(null);
        when(reactionRepo.countByPostId(postId)).thenReturn(3L);

        long count = reactionService.getReactionCount(postId);

        assertThat(count).isEqualTo(3L);
        verify(reactionRepo).countByPostId(postId);
        verify(redisService).setLong("reaction:count:" + postId.toHexString(), 3L, null);
    }
}
