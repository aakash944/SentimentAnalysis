package com.example.demo.sentiment_analysis.realtime_websocket.service;

import com.example.demo.sentiment_analysis.comment.repository.CommentRepo;
import com.example.demo.sentiment_analysis.reaction.repository.ReactionRepo;
import com.example.demo.sentiment_analysis.realtime_websocket.dto.PostRealtimeUpdateDto;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
@ExtendWith(MockitoExtension.class)
class PostRealtimePublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private CommentRepo commentRepo;

    @Mock
    private ReactionRepo reactionRepo;

    @InjectMocks
    private PostRealtimePublisher postRealtimePublisher;

    @Test
    void publishPostUpdateSendsPayloadWithCurrentCounts() {

        ObjectId postId = new ObjectId();

        when(commentRepo.countByPostId(postId))
                .thenReturn(3L);

        when(reactionRepo.countByPostId(postId))
                .thenReturn(5L);

        postRealtimePublisher.publishPostUpdate(
                postId,
                "COMMENT_CREATED",
                "user@example.com"
        );

        ArgumentCaptor<PostRealtimeUpdateDto> payloadCaptor =
                ArgumentCaptor.forClass(PostRealtimeUpdateDto.class);

        verify(messagingTemplate).convertAndSend(eq("/topic/posts/" + postId.toHexString()),
                payloadCaptor.capture()
        );

        PostRealtimeUpdateDto payload =
                payloadCaptor.getValue();

        assertEquals(postId.toHexString(), payload.getPostId());
        assertEquals("COMMENT_CREATED", payload.getEventType());
        assertEquals("user@example.com", payload.getActorEmail());
        assertEquals(3L, payload.getCommentCount());
        assertEquals(5L, payload.getReactionCount());
        assertNotNull(payload.getEmittedAt());
    }
}
