package com.aplemenos.client;

import java.math.BigDecimal;

/**
 * Subset of flight-service's flight JSON that booking-service cares about.
 * Unknown fields are ignored on deserialization.
 */
public record FlightResponse(
        Long id,
        String flightNumber,
        BigDecimal price,
        int availableSeats
) {
}
