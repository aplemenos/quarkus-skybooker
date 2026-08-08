package com.aplemenos;

import com.aplemenos.client.FlightClient;
import com.aplemenos.client.FlightResponse;
import com.aplemenos.client.ReserveRequest;
import com.aplemenos.dto.CreateBookingRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Business logic for bookings. A CDI {@code @ApplicationScoped} bean that calls
 * flight-service (via the injected {@link FlightClient}) to look up the flight
 * and reserve seats before confirming a booking.
 */
@ApplicationScoped
public class BookingService {

    @Inject
    @RestClient
    FlightClient flightClient;

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

        // Call flight-service: (1) get the flight [the request/reply "get" call],
        // then (2) reserve the seats. Translate its error statuses into ours — a
        // raw client WebApplicationException would otherwise surface as a 500.
        //
        // NOTE: reserve (remote) then persist (local) is a dual-write — if the
        // persist below failed, the seats would be reserved with no booking. A
        // production system would use a saga / outbox to compensate. Kept simple
        // here; resilience for an *unreachable* flight-service is added next.
        FlightResponse flight;
        try {
            flight = flightClient.getFlight(request.flightId());
            flightClient.reserve(request.flightId(), new ReserveRequest(request.seats()));
        } catch (WebApplicationException e) {
            int status = e.getResponse() != null ? e.getResponse().getStatus() : 500;
            if (status == 404) {
                throw new NotFoundException("Flight " + request.flightId() + " not found");
            }

            if (status == 409) {
                throw new WebApplicationException(
                        "Flight " + request.flightId() + " has no available seats for "
                                + request.seats() + " passenger(s)", Response.Status.CONFLICT);
            }

            throw e;
        }

        // 3. Persist a CONFIRMED booking, snapshotting the flight number and price.
        Booking booking = new Booking();
        booking.flightId = request.flightId();
        booking.flightNumber = flight.flightNumber();
        booking.passengerName = request.passengerName();
        booking.seats = request.seats();
        booking.totalPrice = flight.price().multiply(BigDecimal.valueOf(request.seats()));
        booking.status = BookingStatus.CONFIRMED;
        booking.persist();

        return booking;
    }
}
