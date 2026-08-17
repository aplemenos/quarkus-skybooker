package com.aplemenos.dto;

/** Body of POST /auth/login. */
public record LoginRequest(String username, String password) {
}
