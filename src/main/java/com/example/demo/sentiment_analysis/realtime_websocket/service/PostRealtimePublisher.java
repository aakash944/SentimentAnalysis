package com.example.demo.sentiment_analysis.realtime_websocket.service;

import com.example.demo.sentiment_analysis.comment.repository.CommentRepo;
import com.example.demo.sentiment_analysis.reaction.repository.ReactionRepo;
import com.example.demo.sentiment_analysis.realtime_websocket.dto.PostRealtimeUpdateDto;
import org.bson.types.ObjectId;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PostRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final CommentRepo commentRepo;
    private final ReactionRepo reactionRepo;

    public PostRealtimePublisher(
            SimpMessagingTemplate messagingTemplate,
            CommentRepo commentRepo,
            ReactionRepo reactionRepo
    ) {
        this.messagingTemplate = messagingTemplate;
        this.commentRepo = commentRepo;
        this.reactionRepo = reactionRepo;
    }

    public void publishPostUpdate(ObjectId postId, String eventType, String actorEmail) {
        long commentCount = commentRepo.countByPostId(postId);
        long reactionCount = reactionRepo.countByPostId(postId);

        PostRealtimeUpdateDto payload = new PostRealtimeUpdateDto(
                postId.toHexString(),
                eventType,
                actorEmail,
                commentCount,
                reactionCount,
                LocalDateTime.now()
        );


        messagingTemplate.convertAndSend("/topic/posts-updates", payload);

        System.out.println("Realtime event emitted");
        System.out.println("Realtime event emitted: " + payload);
    }
}
