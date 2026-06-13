package com.example.demo.sentiment_analysis.reaction.model;

import com.example.demo.sentiment_analysis.enumeration.ReactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "reaction_db")
//@CompoundIndex(name = "unique_user_post",
//        def = "{'userId':1,'postId':1}",
//        unique = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Reaction {
    @Id
    private ObjectId id;
    @Indexed
    private ObjectId userId;

    @Indexed
    private ObjectId postId;
    private ReactionType reactionType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
