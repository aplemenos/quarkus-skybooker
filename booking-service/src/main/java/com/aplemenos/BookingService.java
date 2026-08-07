package com.aplemenos;

import com.aplemenos.dto.CreateBookingRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Business logic for bookings. A CDI {@code @ApplicationScoped} bean.
 *
 * <p>In this milestone a booking is persisted as {@code PENDING}. The next
 * milestone wires the call to flight-service (check availability + reserve seats)
 * which will confirm the booking and snapshot the flight number and price.
 */
@ApplicationScoped
public class BookingService {

    public List<Booking> listAll() {
        return Booking.listAll();
    }

    public Booking findById(Long id) {
        Booking booking = Booking.findById(id);
        if (booking == null) {
            throw new NotFoundException("Booking " + id + " not found");
        }

        return booking;
    }

    @Transactional
    public Booking create(CreateBookingRequest request) {
        if (request.flightId() == null) {
            throw new WebApplicationException("flightId is required", Response.Status.BAD_REQUEST);
        }

        if (request.seats() < 1) {
            throw new WebApplicationException("seats must be at least 1", Response.Status.BAD_REQUEST);
        }

        Booking booking = new Booking();
        booking.flightId = request.flightId();
        booking.passengerName = request.passengerName();
        booking.seats = request.seats();
        booking.status = BookingStatus.PENDING;
        booking.persist();

        return booking;
    }
}
