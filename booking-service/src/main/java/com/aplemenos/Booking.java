package com.aplemenos;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A seat booking on a flight. {@code flightNumber} and {@code totalPrice} are
 * snapshots taken from flight-service at booking time (wired in a later milestone),
 * so the booking record stays correct even if the flight later changes.
 */
@Entity
@Table(name = "bookings")
public class Booking extends PanacheEntity {

    public Long flightId;
    public String flightNumber;
    public String passengerName;
    public int seats;
    public BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    public BookingStatus status;

    public Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
