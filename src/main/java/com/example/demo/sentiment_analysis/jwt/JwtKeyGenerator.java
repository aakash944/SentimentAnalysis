package com.example.demo.sentiment_analysis.jwt;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Base64;

public class JwtKeyGenerator {
    public static void main(String[] args) {
        SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String base64Key = Base64.getEncoder()
                        .encodeToString(secretKey.getEncoded());
        System.out.println("JWT SECRET KEY (COPY THIS):");
        System.out.println(base64Key);
    }
}
