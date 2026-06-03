package com.example.demo.sentiment_analysis.request_dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {

//    @NotBlank(message = "Email not blank")
//    @Email
    private String userEmail;

//    @NotBlank(message = "Password not blank")
//    @Size(min = 6, max = 20)
    private String password;

}