package com.aplemenos.dto;

/** Body of POST /flights/{id}/reserve. */
public record ReserveRequest(int seats) {
}
