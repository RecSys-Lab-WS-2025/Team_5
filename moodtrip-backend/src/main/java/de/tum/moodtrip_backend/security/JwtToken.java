package de.tum.moodtrip_backend.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

public class JwtToken extends AbstractAuthenticationToken {

    private final String token;
    private final Long userId;
    private final UserDetails principal;
    private final Claims claims;

    public JwtToken(String token, Long userId, UserDetails principal, Claims claims) {
        super(principal != null ? principal.getAuthorities() : null);
        this.token = token;
        this.userId = userId;
        this.principal = principal;
        this.claims = claims;
        setAuthenticated(false);
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public Claims getClaims() {
        return claims;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public UserDetails getPrincipal() {
        return principal;
    }

    public JwtToken withAuthenticated(boolean authenticated) {
        JwtToken authenticatedToken = new JwtToken(this.token, this.userId, this.principal, this.claims);
        authenticatedToken.setAuthenticated(authenticated);
        return authenticatedToken;
    }
}