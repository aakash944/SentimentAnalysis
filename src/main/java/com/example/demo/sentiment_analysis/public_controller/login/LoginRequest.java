package com.example.demo.sentiment_analysis.public_controller.login;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Email not blank")
    @Email
    private String userEmail;

    @NotBlank(message = "Password not blank")
    @Size(min = 6, max = 20)
    private String password;
}
