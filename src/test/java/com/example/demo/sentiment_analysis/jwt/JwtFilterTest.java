package com.example.demo.sentiment_analysis.jwt;

import com.example.demo.sentiment_analysis.jwt.utili.JwtUtil;
import com.example.demo.sentiment_analysis.security.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UserDetailsServiceImpl userDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userDetails = new User("alice@test.com", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // shouldNotFilter
    // =========================================================================

    @Test
    @DisplayName("shouldNotFilter: /api/auth/login → true (skips filter)")
    void shouldNotFilter_loginPath_returnsTrue() {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        assertThat(jwtFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter: /api/auth/refresh → true")
    void shouldNotFilter_refreshPath_returnsTrue() {
        when(request.getRequestURI()).thenReturn("/api/auth/refresh");
        assertThat(jwtFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter: /api/auth/logout → true")
    void shouldNotFilter_logoutPath_returnsTrue() {
        when(request.getRequestURI()).thenReturn("/api/auth/logout");
        assertThat(jwtFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter: /api/auth/logout-all → true")
    void shouldNotFilter_logoutAllPath_returnsTrue() {
        when(request.getRequestURI()).thenReturn("/api/auth/logout-all");
        assertThat(jwtFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter: /api/posts → false (must be filtered)")
    void shouldNotFilter_otherPath_returnsFalse() {
        when(request.getRequestURI()).thenReturn("/api/posts");
        assertThat(jwtFilter.shouldNotFilter(request)).isFalse();
    }

    // =========================================================================
    // doFilterInternal — no Authorization header
    // =========================================================================

    @Test
    @DisplayName("doFilterInternal: no Authorization header → chain continues, no authentication set")
    void doFilterInternal_noAuthHeader_continuesChain() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("doFilterInternal: Authorization header without 'Bearer ' prefix → chain continues")
    void doFilterInternal_noBearerPrefix_continuesChain() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // =========================================================================
    // doFilterInternal — valid access token
    // =========================================================================

    @Test
    @DisplayName("doFilterInternal: valid access token → sets authentication in SecurityContext")
    void doFilterInternal_validAccessToken_setsAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtil.extractUserName("valid-token")).thenReturn("alice@test.com");
        when(userDetailsService.loadUserByUsername("alice@test.com")).thenReturn(userDetails);
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractTokenType("valid-token")).thenReturn("access");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("alice@test.com");
        verify(filterChain).doFilter(request, response);
    }

    // =========================================================================
    // doFilterInternal — refresh token rejected
    // =========================================================================

    @Test
    @DisplayName("doFilterInternal: refresh token type → does NOT set authentication")
    void doFilterInternal_refreshToken_doesNotSetAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer refresh-token");
        when(jwtUtil.extractUserName("refresh-token")).thenReturn("alice@test.com");
        when(userDetailsService.loadUserByUsername("alice@test.com")).thenReturn(userDetails);
        when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
        when(jwtUtil.extractTokenType("refresh-token")).thenReturn("refresh");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    // =========================================================================
    // doFilterInternal — invalid / expired token
    // =========================================================================

    @Test
    @DisplayName("doFilterInternal: invalid token → chain continues, no authentication set")
    void doFilterInternal_invalidToken_noAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer expired-token");
        when(jwtUtil.extractUserName("expired-token")).thenReturn("alice@test.com");
        when(userDetailsService.loadUserByUsername("alice@test.com")).thenReturn(userDetails);
        when(jwtUtil.validateToken("expired-token")).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    // =========================================================================
    // doFilterInternal — username extraction returns null
    // =========================================================================

    @Test
    @DisplayName("doFilterInternal: extractUserName returns null → chain continues, no authentication")
    void doFilterInternal_nullUsername_continuesChain() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-token");
        when(jwtUtil.extractUserName("some-token")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(userDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
