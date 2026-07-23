package com.finaxis.financecore.auth;

import com.finaxis.financecore.config.JwtProperties;
import com.finaxis.financecore.user.UserAccount;
import com.finaxis.financecore.user.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void issuesSignedTokenWithExpectedIdentityAndExpiry() {
        Instant now = Instant.parse("2026-07-23T10:00:00Z");
        JwtService service = new JwtService(
                new JwtProperties(
                        "unit-test-secret-long-enough-for-secure-hmac-signing",
                        Duration.ofMinutes(30)
                ),
                Clock.fixed(now, ZoneOffset.UTC)
        );
        UserAccount user = new UserAccount();
        user.setId(42L);
        user.setEmail("analyst@example.com");
        user.setRole(UserRole.ANALYST);

        JwtService.TokenDetails token = service.generateToken(user);
        Claims claims = service.parse(token.token());

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email")).isEqualTo("analyst@example.com");
        assertThat(claims.get("role")).isEqualTo("ANALYST");
        assertThat(token.expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(30)));
    }
}
