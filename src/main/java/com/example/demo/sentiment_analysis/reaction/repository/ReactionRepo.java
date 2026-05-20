package com.example.demo.sentiment_analysis.reaction.repository;

import com.example.demo.sentiment_analysis.reaction.model.Reaction;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ReactionRepo extends MongoRepository<Reaction,ObjectId> {

    Optional<Reaction> findByUserIdAndPostId(ObjectId userId, ObjectId postId);

    void deleteByUserId(ObjectId id);

    Optional<Reaction> findByUserId(ObjectId id);

    void deleteByPostId(ObjectId id);
    List<Reaction> findByPostId(ObjectId postId);
    Slice<Reaction> findByPostIdIn(List<ObjectId> visiblePostIds, Pageable pageable);

    long countByPostId(ObjectId postId);

}
