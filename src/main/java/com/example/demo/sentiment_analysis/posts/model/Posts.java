package com.example.demo.sentiment_analysis.posts.model;

import com.example.demo.sentiment_analysis.posts.enumeration.TypeOfAccess;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "posts_db")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Posts {
    @Id
    private ObjectId id;
    @Indexed
    private ObjectId userId;
    private String title;
    private String content;
    private TypeOfAccess type;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createAt;
}
