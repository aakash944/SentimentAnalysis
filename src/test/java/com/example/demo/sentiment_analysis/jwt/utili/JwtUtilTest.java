package com.example.demo.sentiment_analysis.jwt.utili;

import com.example.demo.sentiment_analysis.jwt.repository.RefreshTokenRepo;
import com.example.demo.sentiment_analysis.jwt.request.LogoutRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @Mock
    private RefreshTokenRepo refreshTokenRepo;

    @InjectMocks
    private JwtUtil jwtUtil;

    // A valid 256-bit Base64-encoded secret for HS256
    private static final String SECRET =
            "dGVzdFNlY3JldEtleVRoYXRJc0xvbmdFbm91Z2hGb3JIUZI1Ng==";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "accessExpirationMs",  3_600_000L); // 1h
        ReflectionTestUtils.setField(jwtUtil, "refreshExpirationMs", 86_400_000L); // 24h
    }

    // =========================================================================
    // generateToken / extractUserName
    // =========================================================================

    @Test
    @DisplayName("generateToken: produces a non-blank token")
    void generateToken_producesToken() {
        String token = jwtUtil.generateToken("alice@test.com");
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("extractUserName: returns the subject embedded in the access token")
    void extractUserName_returnsSubject() {
        String token = jwtUtil.generateToken("alice@test.com");
        assertThat(jwtUtil.extractUserName(token)).isEqualTo("alice@test.com");
    }

    @Test
    @DisplayName("extractUserName: returns null for a malformed token")
    void extractUserName_malformedToken_returnsNull() {
        assertThat(jwtUtil.extractUserName("not.a.jwt")).isNull();
    }

    // =========================================================================
    // generateRefreshToken / extractTokenType
    // =========================================================================

    @Test
    @DisplayName("generateRefreshToken: token type claim is 'refresh'")
    void generateRefreshToken_typeIsRefresh() {
        String token = jwtUtil.generateRefreshToken("alice@test.com");
        assertThat(jwtUtil.extractTokenType(token)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("generateToken: token type claim is 'access'")
    void generateToken_typeIsAccess() {
        String token = jwtUtil.generateToken("alice@test.com");
        assertThat(jwtUtil.extractTokenType(token)).isEqualTo("access");
    }

    // =========================================================================
    // validateToken
    // =========================================================================

    @Test
    @DisplayName("validateToken: valid token returns true")
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken("alice@test.com");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken: malformed token returns false")
    void validateToken_malformedToken_returnsFalse() {
        assertThat(jwtUtil.validateToken("garbage.token.here")).isFalse();
    }

    @Test
    @DisplayName("validateToken: expired token returns false")
    void validateToken_expiredToken_returnsFalse() {
        // Generate with -1ms expiry so it expires immediately
        ReflectionTestUtils.setField(jwtUtil, "accessExpirationMs", -1L);
        String expiredToken = jwtUtil.generateToken("alice@test.com");
        assertThat(jwtUtil.validateToken(expiredToken)).isFalse();
    }

    // =========================================================================
    // extractExpiration
    // =========================================================================

    @Test
    @DisplayName("extractExpiration: returns a future date for a fresh token")
    void extractExpiration_returnsFutureDate() {
        String token = jwtUtil.generateToken("alice@test.com");
        Date expiration = jwtUtil.extractExpiration(token);
        assertThat(expiration).isAfter(new Date());
    }

    // =========================================================================
    // logout
    // =========================================================================

    @Test
    @DisplayName("logout: valid refresh token → deletes from repo")
    void logout_validToken_deletesFromRepo() {
        LogoutRequest request = new LogoutRequest("some-refresh-token");
        jwtUtil.logout(request);
        verify(refreshTokenRepo).deleteByRefreshToken("some-refresh-token");
    }

    @Test
    @DisplayName("logout: null refresh token → does not call repo")
    void logout_nullToken_doesNotCallRepo() {
        LogoutRequest request = new LogoutRequest(null);
        jwtUtil.logout(request);
        verifyNoInteractions(refreshTokenRepo);
    }

    @Test
    @DisplayName("logout: blank refresh token → does not call repo")
    void logout_blankToken_doesNotCallRepo() {
        LogoutRequest request = new LogoutRequest("   ");
        jwtUtil.logout(request);
        verifyNoInteractions(refreshTokenRepo);
    }

    // =========================================================================
    // logoutAllDevices
    // =========================================================================

    @Test
    @DisplayName("logoutAllDevices: deletes all refresh tokens by user email")
    void logoutAllDevices_deletesAllByEmail() {
        jwtUtil.logoutAllDevices("alice@test.com");
        verify(refreshTokenRepo).deleteByUserEmail("alice@test.com");
    }
}
