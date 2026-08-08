package com.aplemenos.client;

/** Body sent to flight-service's POST /flights/{id}/reserve. */
public record ReserveRequest(int seats) {
}
