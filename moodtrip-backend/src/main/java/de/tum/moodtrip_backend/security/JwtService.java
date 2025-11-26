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
     * This is the only method that actually parses the token.
     */
    public Claims parseToken(String token) throws JwtException {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);

        return jws.getBody();
    }

    /**
     * Extract the user ID (subject) from the token.
     * Note: Consider using extractUserId(Claims) with pre-parsed claims for better performance.
     */
    public Long extractUserId(String token) {
        return extractUserId(parseToken(token));
    }

    /**
     * Extract the user ID (subject) from pre-parsed claims.
     */
    public Long extractUserId(Claims claims) {
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new JwtException("Invalid userId (subject) in JWT token", e);
        }
    }

    /**
     * Extract the username claim.
     * Note: Consider using extractUsername(Claims) with pre-parsed claims for better performance.
     */
    public String extractUsername(String token) {
        return extractUsername(parseToken(token));
    }

    /**
     * Extract the username from pre-parsed claims.
     */
    public String extractUsername(Claims claims) {
        return claims.get("username", String.class);
    }

    /**
     * Extract the email claim.
     * Note: Consider using extractEmail(Claims) with pre-parsed claims for better performance.
     */
    public String extractEmail(String token) {
        return extractEmail(parseToken(token));
    }

    /**
     * Extract the email from pre-parsed claims.
     */
    public String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    /**
     * Validate token (signature + expiry).
     * Note: Consider using isTokenValid(Claims) with pre-parsed claims for better performance.
     */
    public boolean isTokenValid(String token) {
        try {
            return isTokenValid(parseToken(token));
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Validate pre-parsed claims (checks expiry).
     */
    public boolean isTokenValid(Claims claims) {
        Date exp = claims.getExpiration();
        return exp != null && exp.after(new Date());
    }
}