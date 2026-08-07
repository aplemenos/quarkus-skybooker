package com.aplemenos.dto;

import com.aplemenos.Flight;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FlightDto(
        Long id,
        String flightNumber,
        String origin,
        String destination,
        LocalDateTime departureTime,
        int totalSeats,
        int availableSeats,
        BigDecimal price
) {
    public static FlightDto from(Flight flight) {
        return new FlightDto(
                flight.id, flight.flightNumber, flight.origin, flight.destination,
                flight.departureTime, flight.totalSeats, flight.availableSeats, flight.price);
    }
}
