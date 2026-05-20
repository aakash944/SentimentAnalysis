package com.example.demo.sentiment_analysis.checker;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthChecker {
    @GetMapping("/health_checker")
    public String status() {
        return "Ok fine";
    }
}
