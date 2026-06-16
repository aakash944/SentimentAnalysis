package com.example.demo.sentiment_analysis.ai.service;

import com.example.demo.sentiment_analysis.ai.SentimentResult;
import com.example.demo.sentiment_analysis.ai.enumeration.SentimentType;
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
            // Validate input
            if (text == null || text.trim().isEmpty()) {
                return new SentimentResult(SentimentType.NEUTRAL, 0.0);
            }

            String prompt = """
                Analyze the sentiment of the following text STRICTLY.
                Return response in JSON format ONLY with exactly these fields:
                {"sentiment": "POSITIVE"|"NEGATIVE"|"NEUTRAL", "confidence": 0.0-1.0}
                
                Rules:
                - If text is gibberish, random characters, or meaningless: return NEUTRAL with confidence 0.2
                - Only return POSITIVE if clearly positive emotions/words
                - Only return NEGATIVE if clearly negative emotions/words
                - Otherwise return NEUTRAL
                
                Text to analyze:
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
