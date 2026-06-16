package com.example.demo.sentiment_analysis.ai.kafka;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import com.example.demo.sentiment_analysis.ai.service.SentimentAiService;
import com.example.demo.sentiment_analysis.ai.SentimentResult;
import com.example.demo.sentiment_analysis.ai.enumeration.SentimentType;
import com.example.demo.sentiment_analysis.comment.repository.CommentRepo;
import com.example.demo.sentiment_analysis.comment.model.Comment;
import com.example.demo.sentiment_analysis.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class SentimentConsumer {
    private final SentimentAiService sentimentAiService;
    private final CommentRepo commentRepo;
    private final RedisService redisService;

    public SentimentConsumer(SentimentAiService sentimentAiService, CommentRepo commentRepo, RedisService redisService) {
        this.sentimentAiService = sentimentAiService;
        this.commentRepo = commentRepo;
        this.redisService = redisService;
    }

    @KafkaListener(topics = "comments-topic", groupId = "sentiment-analysis-group")
    public void processSentiment(@Payload String text, @Header(KafkaHeaders.RECEIVED_KEY) String commentId) {
        try {

            Comment comment = commentRepo.findById(new ObjectId(commentId))
                    .orElseThrow(() -> new RuntimeException("Comment not found"));

            // Check if gibberish FIRST
            if (isGibberishText(text)){
                log.warn("Gibberish comment detected: {}. Deleting.", commentId);
                commentRepo.delete(comment);
                redisService.decrement("comment:count:" + comment.getPostId().toHexString());
                return;
            }

            SentimentResult result = sentimentAiService.analyze(comment.getText());
            comment.setSentiment(result.getSentiment());

            comment.setConfidence(result.getConfidence());

            commentRepo.save(comment);

        } catch (Exception e) {
            log.error("Error processing sentiment for comment: {}", commentId, e);
        }
    }

    private boolean isGibberishText(String text) {
        long specialCharCount = text.chars()
                .filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))
                .count();

        long totalChars = text.length();
        double specialCharRatio = (double) specialCharCount / totalChars;

        // If more than 40% special characters, it's gibberish
        return specialCharRatio > 0.4;
    }



}