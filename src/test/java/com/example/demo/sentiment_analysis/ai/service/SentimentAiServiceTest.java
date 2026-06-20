package com.example.demo.sentiment_analysis.ai.service;

import com.example.demo.sentiment_analysis.ai.SentimentResult;
import com.example.demo.sentiment_analysis.ai.enumeration.SentimentType;
import com.example.demo.sentiment_analysis.exception.SentimentAnalysisException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SentimentAiServiceTest {

    // --- Fluent chain mocks ---
    @Mock private ChatClient chatClient;
    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.ChatClientRequestSpec userSpec;
    @Mock private ChatClient.CallResponseSpec callSpec;

    private SentimentAiService sentimentAiService;

    @BeforeEach
    void setUp() {
        sentimentAiService = new SentimentAiService(chatClient);

        // Wire up the fluent chain: chatClient.prompt().user(...).call()
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(userSpec);
        when(userSpec.call()).thenReturn(callSpec);
    }

    // -------------------------------------------------------------------------
    // analyze() — success cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("analyze: positive text returns POSITIVE sentiment")
    void analyze_positiveText_returnsPositiveResult() {
        SentimentResult expected = new SentimentResult(SentimentType.POSITIVE, 0.95);
        when(callSpec.entity(SentimentResult.class)).thenReturn(expected);

        SentimentResult result = sentimentAiService.analyze("I love this product!");

        assertThat(result.getSentiment()).isEqualTo(SentimentType.POSITIVE);
        assertThat(result.getConfidence()).isEqualTo(0.95);
        verify(chatClient).prompt();
        verify(requestSpec).user(anyString());
        verify(userSpec).call();
        verify(callSpec).entity(SentimentResult.class);
    }

    @Test
    @DisplayName("analyze: negative text returns NEGATIVE sentiment")
    void analyze_negativeText_returnsNegativeResult() {
        SentimentResult expected = new SentimentResult(SentimentType.NEGATIVE, 0.88);
        when(callSpec.entity(SentimentResult.class)).thenReturn(expected);

        SentimentResult result = sentimentAiService.analyze("This is terrible and awful.");

        assertThat(result.getSentiment()).isEqualTo(SentimentType.NEGATIVE);
        assertThat(result.getConfidence()).isEqualTo(0.88);
    }

    @Test
    @DisplayName("analyze: neutral text returns NEUTRAL sentiment")
    void analyze_neutralText_returnsNeutralResult() {
        SentimentResult expected = new SentimentResult(SentimentType.NEUTRAL, 0.60);
        when(callSpec.entity(SentimentResult.class)).thenReturn(expected);

        SentimentResult result = sentimentAiService.analyze("The sky is blue.");

        assertThat(result.getSentiment()).isEqualTo(SentimentType.NEUTRAL);
        assertThat(result.getConfidence()).isEqualTo(0.60);
    }

    // -------------------------------------------------------------------------
    // analyze() — null / empty input (short-circuits before calling AI)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("analyze: null input returns NEUTRAL with 0.0 confidence without calling AI")
    void analyze_nullText_returnsNeutralWithoutCallingAi() {
        SentimentResult result = sentimentAiService.analyze(null);

        assertThat(result.getSentiment()).isEqualTo(SentimentType.NEUTRAL);
        assertThat(result.getConfidence()).isEqualTo(0.0);
        verifyNoInteractions(chatClient);
    }

    @Test
    @DisplayName("analyze: empty string returns NEUTRAL with 0.0 confidence without calling AI")
    void analyze_emptyText_returnsNeutralWithoutCallingAi() {
        SentimentResult result = sentimentAiService.analyze("   ");

        assertThat(result.getSentiment()).isEqualTo(SentimentType.NEUTRAL);
        assertThat(result.getConfidence()).isEqualTo(0.0);
        verifyNoInteractions(chatClient);
    }

    // -------------------------------------------------------------------------
    // analyze() — exception case
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("analyze: AI call throws exception → wraps into SentimentAnalysisException")
    void analyze_aiThrowsException_throwsSentimentAnalysisException() {
        when(callSpec.entity(SentimentResult.class))
                .thenThrow(new RuntimeException("Ollama unavailable"));

        assertThatThrownBy(() -> sentimentAiService.analyze("Some valid text"))
                .isInstanceOf(SentimentAnalysisException.class)
                .hasMessage("Sentiment analysis unavailable");
    }

    @Test
    @DisplayName("analyze: prompt is built with user text included")
    void analyze_promptContainsUserText() {
        SentimentResult expected = new SentimentResult(SentimentType.POSITIVE, 0.9);
        when(callSpec.entity(SentimentResult.class)).thenReturn(expected);

        sentimentAiService.analyze("Great day!");

        // Verify the user text was embedded in the prompt passed to .user()
        verify(requestSpec).user(contains("Great day!"));
    }
}
