package com.example.demo.sentiment_analysis.ai;

import com.example.demo.sentiment_analysis.ai.enumeration.SentimentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SentimentResult {
    SentimentType sentiment;
    Double confidence;
}
