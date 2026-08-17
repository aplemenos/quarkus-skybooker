package com.aplemenos.dto;

/** Response of POST /auth/login — the signed JWT. */
public record TokenResponse(String token, String tokenType, long expiresInSeconds) {

    public static TokenResponse bearer(String token, long expiresInSeconds) {
        return new TokenResponse(token, "Bearer", expiresInSeconds);
    }
}
