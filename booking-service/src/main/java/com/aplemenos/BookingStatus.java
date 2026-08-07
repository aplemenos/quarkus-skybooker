package com.aplemenos;

public enum BookingStatus {
    /** Seats confirmed with flight-service. */
    CONFIRMED,
    /** Created but not yet confirmed (e.g. flight-service was unreachable). */
    PENDING,
    /** Rejected — e.g. the flight had no seats. */
    REJECTED
}
