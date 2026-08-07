package com.aplemenos.dto;

import com.aplemenos.Booking;
import com.aplemenos.BookingStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record BookingDto(
        Long id,
        Long flightId,
        String flightNumber,
        String passengerName,
        int seats,
        BigDecimal totalPrice,
        BookingStatus status,
        Instant createdAt
) {
    public static BookingDto from(Booking booking) {
        return new BookingDto(
                booking.id, booking.flightId, booking.flightNumber, booking.passengerName,
                booking.seats, booking.totalPrice, booking.status, booking.createdAt);
    }
}
