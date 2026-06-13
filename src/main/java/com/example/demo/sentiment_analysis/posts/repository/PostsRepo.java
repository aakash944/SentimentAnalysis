package com.example.demo.sentiment_analysis.posts.repository;

import com.example.demo.sentiment_analysis.posts.enumeration.TypeOfAccess;
import com.example.demo.sentiment_analysis.posts.model.Posts;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;




public interface PostsRepo extends MongoRepository<Posts, ObjectId> {
    void deleteByUserId(ObjectId userId);
    Slice<Posts> findByTypeOrUserId(TypeOfAccess typeOfAccess, ObjectId id, Pageable pageable);
}
