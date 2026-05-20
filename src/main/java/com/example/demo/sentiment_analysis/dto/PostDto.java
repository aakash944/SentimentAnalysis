package com.example.demo.sentiment_analysis.dto;

import com.example.demo.sentiment_analysis.enumeration.TypeOfAccess;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostDto {
    private ObjectId id;
    private String title;
    private String content;
    private TypeOfAccess typeOfAccess;
}
