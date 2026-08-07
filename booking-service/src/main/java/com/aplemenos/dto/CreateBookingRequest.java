package com.aplemenos.dto;

/** Body of POST /bookings. */
public record CreateBookingRequest(
        Long flightId,
        String passengerName,
        int seats
) {
}
