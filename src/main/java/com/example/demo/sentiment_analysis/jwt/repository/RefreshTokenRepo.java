package com.example.demo.sentiment_analysis.jwt.repository;

import com.example.demo.sentiment_analysis.jwt.model.RefreshToken;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RefreshTokenRepo extends MongoRepository<RefreshToken, ObjectId> {
    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    void deleteByRefreshToken(String refreshToken);

    void deleteByUserEmail(String userEmail);
}
