package com.aplemenos;

import java.math.BigDecimal;

/**
 * Result of trying to reserve seats on flight-service.
 *
 * @param confirmed true if flight-service confirmed the reservation; false if it
 *                  was unreachable and the fallback ran (booking will be PENDING).
 */
public record FlightReservation(String flightNumber, BigDecimal price, boolean confirmed) {

    static FlightReservation confirmed(String flightNumber, BigDecimal price) {
        return new FlightReservation(flightNumber, price, true);
    }

    static FlightReservation unavailable() {
        return new FlightReservation(null, null, false);
    }
}
