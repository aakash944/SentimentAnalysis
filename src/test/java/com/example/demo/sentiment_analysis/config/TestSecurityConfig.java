package com.example.demo.sentiment_analysis.config;

import com.example.demo.sentiment_analysis.jwt.utili.JwtUtil;
import com.example.demo.sentiment_analysis.security.UserDetailsServiceImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.mockito.Mockito.mock;

/**
 * Shared test configuration for all @WebMvcTest slices.
 * - Mocks JwtUtil + UserDetailsServiceImpl so JwtFilter can be instantiated
 * - Mocks MongoDatabaseFactory so DemoApplication's transactionManager bean is satisfied
 * - Provides a permissive SecurityFilterChain replacing SpringSecurity
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public JwtUtil jwtUtil() {
        return mock(JwtUtil.class);
    }

    @Bean
    public UserDetailsServiceImpl userDetailsService() {
        return mock(UserDetailsServiceImpl.class);
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory() {
        return mock(MongoDatabaseFactory.class);
    }

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
