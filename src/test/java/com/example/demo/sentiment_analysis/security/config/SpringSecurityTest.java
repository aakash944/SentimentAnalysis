package com.example.demo.sentiment_analysis.security.config;

import com.example.demo.sentiment_analysis.jwt.JwtFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringSecurityTest {

    @Mock
    private JwtFilter jwtFilter;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock(answer = Answers.RETURNS_SELF)
    private HttpSecurity httpSecurity;

    @Mock
    private DefaultSecurityFilterChain securityFilterChain;

    @InjectMocks
    private SpringSecurity springSecurity;

    @Test
    void securityFilterChainBuildsHttpSecurityAndAddsJwtFilter() throws Exception {
        when(httpSecurity.build()).thenReturn(securityFilterChain);

        SecurityFilterChain result = springSecurity.securityFilterChain(httpSecurity);

        assertSame(securityFilterChain, result);
        verify(httpSecurity).csrf(any());
        verify(httpSecurity).sessionManagement(any());
        verify(httpSecurity).authorizeHttpRequests(any());
        verify(httpSecurity).addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        verify(httpSecurity).build();
    }

    @Test
    void authenticationManagerReturnsManagerFromConfiguration() throws Exception {
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        AuthenticationManager result = springSecurity.authenticationManager(authenticationConfiguration);

        assertSame(authenticationManager, result);
        verify(authenticationConfiguration).getAuthenticationManager();
    }

    @Test
    void authenticationManagerPropagatesConfigurationException() throws Exception {
        Exception failure = new Exception("authentication manager failed");
        when(authenticationConfiguration.getAuthenticationManager()).thenThrow(failure);

        Exception exception = org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> springSecurity.authenticationManager(authenticationConfiguration)
        );

        assertSame(failure, exception);
    }

    @Test
    void passwordEncoderReturnsBCryptPasswordEncoder() {
        PasswordEncoder result = springSecurity.passwordEncoder();

        assertInstanceOf(BCryptPasswordEncoder.class, result);
        assertTrue(result.matches("secret", result.encode("secret")));
    }
}
