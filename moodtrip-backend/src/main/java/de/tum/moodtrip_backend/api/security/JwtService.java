package de.tum.moodtrip_backend.api.security;

import de.tum.moodtrip_backend.core.model.UserProfile;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class JwtService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);

    private final Key signingKey;
    private final long expirationMinutes;
    private final long refreshGracePeriodMinutes;
    private final String issuer;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes:60}") long expirationMinutes,
            @Value("${app.jwt.issuer:moodtrip-backend}") String issuer,
            @Value("${app.jwt.refresh-grace-period-minutes:10080}") long refreshGracePeriodMinutes
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
        this.issuer = issuer;
        this.refreshGracePeriodMinutes = refreshGracePeriodMinutes;
    }

    /**
     * Generate a JWT for a logged-in user.
     */
    public String generateToken(UserProfile user) {
        LOGGER.info("Generating JWT token for user ID: {}", user.id());
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
            LOGGER.error("Invalid userId format in token subject: {}", claims.getSubject());
            throw new JwtException("Invalid userId (subject) in JWT token", e);
        }
    }

    /**
     * Extract the user ID from Authentication object.
     * Convenience method for controllers.
     */
    public Long extractUserId(Authentication authentication) {
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
     *
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
     *
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
     * Generate a new token based on an old (potentially expired) token.
     * Only valid if the signature is correct and it was issued by us.
     */
    public String refreshCurrentToken(String token) {
        Claims claims = parseClaimsIgnoreExpiration(token);
        
        // Security Check: Only allow refresh within a grace period (e.g. 7 days) from original issuance
        Date issuedAt = claims.getIssuedAt();
        if (issuedAt != null) {
            Instant maxRefreshTime = issuedAt.toInstant().plus(refreshGracePeriodMinutes, ChronoUnit.MINUTES);
            if (Instant.now().isAfter(maxRefreshTime)) {
                LOGGER.warn("Refresh rejected: Token issued at {} is beyond the grace period of {} minutes", 
                        issuedAt, refreshGracePeriodMinutes);
                throw new JwtException("Session is too old to be extended. Please sign in again.");
            }
        }

        String userIdStr = claims.getSubject();
        String username = claims.get("username", String.class);
        String email = claims.get("email", String.class);

        LOGGER.info("Refreshing JWT token for user ID: {}", userIdStr);
        
        Instant now = Instant.now();
        Instant exp = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .setSubject(userIdStr)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim("username", username)
                .claim("email", email)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims parseClaimsIgnoreExpiration(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return e.getClaims();
        } catch (JwtException e) {
            LOGGER.error("Failed to parse token even with expiration ignored: {}", e.getMessage());
            throw e;
        }
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
            LOGGER.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}