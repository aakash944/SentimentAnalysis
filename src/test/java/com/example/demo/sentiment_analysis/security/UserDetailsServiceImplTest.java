package com.example.demo.sentiment_analysis.security;

import com.example.demo.sentiment_analysis.user.model.Users;
import com.example.demo.sentiment_analysis.user.repository.UserRepo;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsernameReturnsUserDetailsWhenUserExists() {
        Users user = new Users(
                new ObjectId(),
                "admin@example.com",
                "encoded-password",
                LocalDateTime.now(),
                List.of("USER", "ADMIN")
        );

        when(userRepo.findByUserEmail("admin@example.com")).thenReturn(user);

        UserDetails result = userDetailsService.loadUserByUsername("admin@example.com");

        assertEquals("admin@example.com", result.getUsername());
        assertEquals("encoded-password", result.getPassword());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
        assertTrue(result.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
        verify(userRepo).findByUserEmail("admin@example.com");
    }

    @Test
    void loadUserByUsernameThrowsUsernameNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepo.findByUserEmail("missing@example.com")).thenReturn(null);

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing@example.com")
        );

        assertEquals("User Not found exception", exception.getMessage());
        verify(userRepo).findByUserEmail("missing@example.com");
    }
}
