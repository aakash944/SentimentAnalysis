package com.example.demo.sentiment_analysis.ai.kafka;

import com.example.demo.sentiment_analysis.ai.SentimentResult;
import com.example.demo.sentiment_analysis.ai.enumeration.SentimentType;
import com.example.demo.sentiment_analysis.ai.service.SentimentAiService;
import com.example.demo.sentiment_analysis.comment.model.Comment;
import com.example.demo.sentiment_analysis.comment.repository.CommentRepo;
import com.example.demo.sentiment_analysis.redis.service.RedisService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SentimentConsumerTest {

    @Mock private SentimentAiService sentimentAiService;
    @Mock private CommentRepo commentRepo;
    @Mock private RedisService redisService;

    @InjectMocks
    private SentimentConsumer sentimentConsumer;

    private ObjectId commentId;
    private ObjectId postId;
    private Comment comment;

    @BeforeEach
    void setUp() {
        commentId = new ObjectId();
        postId    = new ObjectId();

        comment = new Comment();
        comment.setId(commentId);
        comment.setPostId(postId);
        comment.setText("I love this!");
    }

    @Test
    @DisplayName("processSentiment: valid text → analyzes sentiment and saves comment")
    void processSentiment_validText_analyzesSentimentAndSaves() {
        when(commentRepo.findById(commentId)).thenReturn(Optional.of(comment));
        SentimentResult result = new SentimentResult(SentimentType.POSITIVE, 0.92);
        when(sentimentAiService.analyze(comment.getText())).thenReturn(result);

        sentimentConsumer.processSentiment("I love this!", commentId.toHexString());

        verify(sentimentAiService).analyze("I love this!");
        verify(commentRepo).save(comment);
        // Sentiment and confidence must be set on the comment before saving
        assert comment.getSentiment() == SentimentType.POSITIVE;
        assert comment.getConfidence() == 0.92;
        verifyNoInteractions(redisService);
    }

    // -------------------------------------------------------------------------
    // processSentiment() — gibberish detection
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("processSentiment: gibberish text (>40% special chars) → deletes comment, decrements Redis")
    void processSentiment_gibberishText_deletesCommentAndDecrementsRedis() {
        // Text with >40% special characters
        String gibberish = "!!!@@@###$$$%%%";
        when(commentRepo.findById(commentId)).thenReturn(Optional.of(comment));

        sentimentConsumer.processSentiment(gibberish, commentId.toHexString());

        verify(commentRepo).delete(comment);
        verify(redisService).decrement("comment:count:" + postId.toHexString());
        verifyNoInteractions(sentimentAiService);
        verify(commentRepo, never()).save(any());
    }

    @Test
    @DisplayName("processSentiment: text at exactly 40% special chars → NOT treated as gibberish")
    void processSentiment_textAtBoundary_notGibberish() {
        // "aaaa!!!!" — 4 letters, 4 special = exactly 50% special → gibberish
        // "aaaaaa!!" — 6 letters, 2 special = 25% special → NOT gibberish
        String borderText = "aaaaaa!!"; // 25% special chars — should pass through
        comment.setText(borderText);
        when(commentRepo.findById(commentId)).thenReturn(Optional.of(comment));
        SentimentResult result = new SentimentResult(SentimentType.NEUTRAL, 0.5);
        when(sentimentAiService.analyze(borderText)).thenReturn(result);

        sentimentConsumer.processSentiment(borderText, commentId.toHexString());

        verify(sentimentAiService).analyze(borderText);
        verify(commentRepo).save(comment);
        verify(commentRepo, never()).delete(any());
    }

    // -------------------------------------------------------------------------
    // processSentiment() — comment not found
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("processSentiment: comment not found → logs error, does not save or delete")
    void processSentiment_commentNotFound_handlesGracefully() {
        when(commentRepo.findById(commentId)).thenReturn(Optional.empty());

        // Should NOT throw — exception is caught and logged internally
        sentimentConsumer.processSentiment("Some text", commentId.toHexString());

        verify(commentRepo, never()).save(any());
        verify(commentRepo, never()).delete(any());
        verifyNoInteractions(sentimentAiService);
        verifyNoInteractions(redisService);
    }

    // -------------------------------------------------------------------------
    // processSentiment() — AI service throws exception
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("processSentiment: AI service throws → logs error, does not save comment")
    void processSentiment_aiServiceThrows_handlesGracefully() {
        when(commentRepo.findById(commentId)).thenReturn(Optional.of(comment));
        when(sentimentAiService.analyze(anyString()))
                .thenThrow(new RuntimeException("AI unavailable"));

        // Should NOT throw — exception is caught and logged internally
        sentimentConsumer.processSentiment("I love this!", commentId.toHexString());

        verify(sentimentAiService).analyze("I love this!");
        verify(commentRepo, never()).save(any());
        verify(commentRepo, never()).delete(any());
        verifyNoInteractions(redisService);
    }

    // -------------------------------------------------------------------------
    // processSentiment() — Redis decrement on gibberish uses correct key
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("processSentiment: Redis key uses postId from the comment, not commentId")
    void processSentiment_gibberish_redisKeyUsesPostId() {
        String gibberish = "####$$$$%%%%";
        when(commentRepo.findById(commentId)).thenReturn(Optional.of(comment));

        sentimentConsumer.processSentiment(gibberish, commentId.toHexString());

        verify(redisService).decrement("comment:count:" + postId.toHexString());
        verify(redisService, never()).decrement("comment:count:" + commentId.toHexString());
    }

    // -------------------------------------------------------------------------
    // processSentiment() — confidence is persisted alongside sentiment
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("processSentiment: confidence value from AI result is saved on comment")
    void processSentiment_confidenceIsPersisted() {
        when(commentRepo.findById(commentId)).thenReturn(Optional.of(comment));
        SentimentResult result = new SentimentResult(SentimentType.NEGATIVE, 0.77);
        when(sentimentAiService.analyze(comment.getText())).thenReturn(result);

        sentimentConsumer.processSentiment("I love this!", commentId.toHexString());

        verify(commentRepo).save(argThat(saved ->
                saved.getSentiment() == SentimentType.NEGATIVE
                && saved.getConfidence() == 0.77
        ));
    }
}
