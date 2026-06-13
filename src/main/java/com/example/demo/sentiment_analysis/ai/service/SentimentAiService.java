package com.example.demo.sentiment_analysis.ai.service;

import com.example.demo.sentiment_analysis.ai.SentimentResult;
import com.example.demo.sentiment_analysis.exception.SentimentAnalysisException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.ChatClient;

@Slf4j
@Service
public class SentimentAiService {
    private final ChatClient chatClient;
    public SentimentAiService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public SentimentResult analyze(String text) {
        try {
            String prompt = """
                    Analyze the sentiment of the following comment.
                    Return ONLY a result that fits this structure:
                    sentiment: POSITIVE, NEGATIVE, or NEUTRAL
                    confidence: a number between 0 and 1

                    Comment:
                    %s
                    """.formatted(text);

            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(SentimentResult.class);

        } catch (Exception e) {
            log.error("Sentiment analysis failed for text: {}", text, e);
            throw new SentimentAnalysisException("Sentiment analysis unavailable");
        }
    }
}
