package com.example.demo.sentiment_analysis.checker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class HealthChecker {
    @GetMapping("/health_checker")
    public String status() {
        log.info("Hello i am in Health_Checker");
        return "Ok fine";
    }
}
