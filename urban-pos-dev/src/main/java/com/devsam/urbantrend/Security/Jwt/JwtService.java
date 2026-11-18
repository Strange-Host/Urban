package com.devsam.urbantrend.Security.Jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final String SESSION_CLAIM = "sid";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpiration;

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractSessionId(String token) {
        try {
            String sid = extractClaim(token, claims -> claims.get(SESSION_CLAIM, String.class));
            return sid == null ? null : UUID.fromString(sid);
        } catch (Exception e) {
            return null;
        }
    }

    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateAccessToken(String email, String role, List<String> authorities) {
        return generateAccessToken(email, role, authorities, Map.of());
    }

    /**
     * @param profile extra identity claims (id, name, phoneNumber) so the frontend can
     *                show who is signed in and ask for "my" data without another call
     */
    public String generateAccessToken(String email, String role, List<String> authorities,
                                      Map<String, Object> profile) {
        return generateAccessToken(email, role, authorities, profile, null);
    }

    /** @param sessionId the device session this token belongs to; null for a sessionless token */
    public String generateAccessToken(String email, String role, List<String> authorities,
                                      Map<String, Object> profile, UUID sessionId) {
        Map<String, Object> claims = new HashMap<>();
        if (profile != null) {
            claims.putAll(profile);
        }
        claims.put("role", role);
        claims.put("authorities", authorities); // ✅ embedded in token
        claims.put(TOKEN_TYPE_CLAIM, TYPE_ACCESS);
        if (sessionId != null) {
            claims.put(SESSION_CLAIM, sessionId.toString());
        }
        return buildToken(email, claims, accessTokenExpiration);
    }

    public String generateRefreshToken(String email) {
        return generateRefreshToken(email, null);
    }

    public String generateRefreshToken(String email, UUID sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, TYPE_REFRESH);
        if (sessionId != null) {
            claims.put(SESSION_CLAIM, sessionId.toString());
        }
        return buildToken(email, claims, refreshTokenExpiration);
    }

    public boolean isAccessToken(String token) {
        try {
            return TYPE_ACCESS.equals(extractClaim(token, claims -> claims.get(TOKEN_TYPE_CLAIM, String.class)));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            return TYPE_REFRESH.equals(extractClaim(token, claims -> claims.get(TOKEN_TYPE_CLAIM, String.class)));
        } catch (Exception e) {
            return false;
        }
    }

    private String buildToken(String email, Map<String, Object> claims, long expiration) {

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token) {

        try {

            if (!token.contains(".")) {
                return false;
            }

            Claims claims = extractAllClaims(token);

            return !claims.getExpiration().before(new Date());

        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {

        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("jwt.secret is not configured");
        }

        byte[] keyBytes = null;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey);
        } catch (Exception ignored) {
            // Not valid Base64 - fall back to the raw secret below.
        }

        if (keyBytes == null || keyBytes.length < 32) {
            keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes (256 bits)");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}