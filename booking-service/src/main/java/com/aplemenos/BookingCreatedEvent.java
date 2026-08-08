package com.aplemenos;

/** CDI event fired after a booking is persisted. */
public record BookingCreatedEvent(
        Long bookingId,
        Long flightId,
        int seats,
        BookingStatus status
) {
}
