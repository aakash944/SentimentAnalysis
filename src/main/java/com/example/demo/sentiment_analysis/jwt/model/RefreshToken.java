package com.example.demo.sentiment_analysis.jwt.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "refresh_tokens")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {
    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private String refreshToken;

    private ObjectId userId;

    private String userEmail;

    @Indexed(expireAfterSeconds = 0)
    private Date expiresAt;

    private Date createdAt;
}
