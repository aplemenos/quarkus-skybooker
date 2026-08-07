package com.aplemenos.dto;

public record AvailabilityResponse(
        Long flightId,
        int requestedSeats,
        boolean available,
        int availableSeats
) {
}
