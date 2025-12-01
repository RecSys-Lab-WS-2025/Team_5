package de.tum.moodtrip_backend.security;

import de.tum.moodtrip_backend.core.model.UserProfile;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final Key signingKey;
    private final long expirationMinutes;
    private final String issuer;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes:60}") long expirationMinutes,
            @Value("${app.jwt.issuer:moodtrip-backend}") String issuer
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
        this.issuer = issuer;
    }

    /**
     * Generate a JWT for a logged-in user.
     */
    public String generateToken(UserProfile user) {
        Instant now = Instant.now();
        Instant exp = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .setSubject(String.valueOf(user.id()))
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim("username", user.username())
                .claim("email", user.email())
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Parse and validate the JWT. Throws JwtException if invalid.
     */
    private Claims parseToken(String token) throws JwtException {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);

        return jws.getBody();
    }

    /**
     * Extract the user ID (subject) from the token.
     */
    public Long extractUserId(String token) {
        Claims claims = parseToken(token);
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new JwtException("Invalid userId (subject) in JWT token", e);
        }
    }

    /**
     * Extract the user ID from Authentication object.
     * Convenience method for controllers.
     */
    public Long extractUserId(org.springframework.security.core.Authentication authentication) {
        if (authentication == null) {
            throw new JwtException("Authentication is null");
        }
        
        if (!(authentication instanceof JwtToken jwtToken)) {
            throw new JwtException("Authentication is not a JwtToken instance");
        }
        
        return jwtToken.getUserId();
    }

    /**
     * Extract the username claim.
     * @throws JwtException if the username claim is missing or empty
     */
    public String extractUsername(String token) {
        Claims claims = parseToken(token);
        String username = claims.get("username", String.class);
        if (username == null || username.isEmpty()) {
            throw new JwtException("Missing or empty 'username' claim in JWT token");
        }
        return username;
    }

    /**
     * Extract the email claim.
     * @throws JwtException if the email claim is missing or empty
     */
    public String extractEmail(String token) {
        Claims claims = parseToken(token);
        String email = claims.get("email", String.class);
        if (email == null || email.isEmpty()) {
            throw new JwtException("Missing or empty 'email' claim in JWT token");
        }
        return email;
    }

    /**
     * Validate token (signature + expiry).
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseToken(token);
            Date exp = claims.getExpiration();
            return exp != null && exp.after(new Date());
        } catch (JwtException e) {
            return false;
        }
    }
}