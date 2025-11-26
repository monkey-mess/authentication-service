package ru.balybin.monkey_backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.UUID;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class TokenProvider {

    private final SecretKey key;

    public TokenProvider(@Value("${auth.jwt.secret}") String jwtSecret) {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(Authentication auth, UUID userId) {
        // Получаем роли пользователя
        String authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        String jwt= Jwts.builder().issuer("MONKEY_MESS")
                .issuedAt(new Date()).expiration(new Date(new Date().getTime() + 86400000))
                .claim("email", auth.getName())
                .claim("userId", userId != null ? userId.toString() : null)
                .claim("authorities", authorities) // Добавляем authorities в токен
                .signWith(key)
                .compact();
        return jwt;
    }

    public String getEmailFromToken(String jwt) {
        // Обрабатываем случай, когда токен приходит с "Bearer " префиксом
        if (jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7);
        }

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();

        String email = String.valueOf(claims.get("email"));
        return email;
    }
}