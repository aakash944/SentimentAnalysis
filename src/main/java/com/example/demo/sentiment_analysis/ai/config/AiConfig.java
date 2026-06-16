package com.example.demo.sentiment_analysis.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientResponseException;

@Configuration
public class AiConfig {
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.defaultSystem(
                        "You are sentiment analyzer")
                .build();
    }
    @Bean
    public RetryTemplate retryTemplate() {
        return new RetryTemplate();
    }
//        @Bean
//    public ResponseErrorHandler responseErrorHandler() {
//        return new ResponseErrorHandler() {
//            @Override
//            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) throws java.io.IOException {
//                return response.getStatusCode().isError();
//            }
//
//            @Override
//            public void handleError(org.springframework.http.client.ClientHttpResponse response) throws java.io.IOException {
//                throw new RestClientResponseException(
//                        "HTTP " + response.getStatusCode(),
//                        response.getStatusCode().value(),
//                        response.getStatusText(),
//                        response.getHeaders(),
//                        null,
//                        null
//                );
//            }
//        };
//    }
}
