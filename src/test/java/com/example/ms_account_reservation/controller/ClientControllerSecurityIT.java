package com.example.ms_account_reservation.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ClientControllerSecurityIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired MockMvc mockMvc;

    @Value("${app.jwt.secret}")
    String jwtSecret;


    @Test
    void requestWithExpiredJwt_isRejected() throws Exception {
        String expired = tokenWithExpiration(
                Instant.now().minusSeconds(60),
                "user",
                List.of("USER")
        );

        mockMvc.perform(get("/clients")
                        .header("Authorization", "Bearer " + expired)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestWithValidJwt_isAllowed() throws Exception {
        String valid = tokenWithExpiration(
                Instant.now().plusSeconds(3600),
                "user",
                List.of("USER")
        );

        mockMvc.perform(get("/clients")
                        .header("Authorization", "Bearer " + valid)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void requestWithJwtSignedByWrongKey_isRejected() throws Exception {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "another-secret-another-secret-32b!".getBytes(StandardCharsets.UTF_8));

        String forged = Jwts.builder()
                .subject("user")
                .claim("roles", List.of("USER"))
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(otherKey, Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(get("/clients").header("Authorization", "Bearer " + forged))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestWithJwtOfWrongAuthority_isForbidden() throws Exception {
        String token = tokenWithExpiration(
                Instant.now().plusSeconds(3600),
                "user",
                List.of("NOT_USER_ROLE")
        );

        mockMvc.perform(get("/clients")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }


    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    private String tokenWithExpiration(Instant expiresAt, String subject, List<String> roles) {
        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .issuedAt(Date.from(expiresAt.minusSeconds(3600)))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }
}
