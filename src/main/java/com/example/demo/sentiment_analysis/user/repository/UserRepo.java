package com.example.demo.sentiment_analysis.user.repository;


import com.example.demo.sentiment_analysis.user.model.Users;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepo extends MongoRepository<Users, ObjectId> {

    Users findByUserEmail(String userEmail);
}
