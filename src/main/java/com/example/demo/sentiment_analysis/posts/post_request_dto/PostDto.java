package com.example.demo.sentiment_analysis.posts.post_request_dto;


import com.example.demo.sentiment_analysis.posts.enumeration.TypeOfAccess;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostDto {
    private ObjectId id;
    @NotBlank
    private String title;
    @NotBlank
    @Size(max = 200)
    private String content;
    @NotBlank
    private TypeOfAccess typeOfAccess;
}
