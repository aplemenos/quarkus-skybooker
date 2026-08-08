package com.aplemenos;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Business logic for flights. A CDI {@code @ApplicationScoped} bean — a single
 * instance is shared and injected into {@code FlightResource}.
 */
@ApplicationScoped
public class FlightService {

    private static final Logger LOG = Logger.getLogger(FlightService.class);

    /** Max seats allowed in a single reservation — from MicroProfile Config. */
    @Inject
    @ConfigProperty(name = "flight.max-seats-per-reservation")
    int maxSeatsPerReservation;

    /** Micrometer registry — exposes custom metrics at /q/metrics (Prometheus). */
    @Inject
    MeterRegistry meterRegistry;

    public List<Flight> listAll() {
        return Flight.listAll();
    }

    public Flight findById(Long id) {
        Flight flight = Flight.findById(id);
        if (flight == null) {
            throw new NotFoundException("Flight " + id + " not found");
        }

        return flight;
    }

    public boolean hasSeats(Long id, int seats) {
        return findById(id).availableSeats >= seats;
    }

    /**
     * Atomically reserves seats. The conditional {@code UPDATE ... WHERE
     * availableSeats >= seats} means concurrent reservations can never oversell:
     * the database serializes the updates, and a losing request affects 0 rows.
     */
    @Transactional
    public Flight reserve(Long id, int seats) {
        if (seats < 1) {
            throw new WebApplicationException("Seats must be at least 1", Response.Status.BAD_REQUEST);
        }
        if (seats > maxSeatsPerReservation) {
            throw new WebApplicationException(
                    "Cannot reserve more than " + maxSeatsPerReservation + " seats at once",
                    Response.Status.BAD_REQUEST);
        }

        Flight flight = findById(id); // 404 if the flight does not exist

        LOG.infof("Reserving %d seat(s) on flight %d (%s)", seats, id, flight.flightNumber);

        long updated = Flight.update(
                "availableSeats = availableSeats - ?1 where id = ?2 and availableSeats >= ?1",
                seats, id);
        if (updated == 0) {
            LOG.warnf("Reservation rejected: flight %d has fewer than %d seats available", id, seats);

            throw new WebApplicationException(
                    "Not enough seats on flight " + id, Response.Status.CONFLICT);
        }

        // The bulk update bypassed the persistence context; reload the fresh state.
        Flight.getEntityManager().refresh(flight);

        // Custom metric: total seats reserved (flights_seats_reserved_total).
        meterRegistry.counter("flights.seats.reserved").increment(seats);

        LOG.infof("Reserved %d seat(s) on flight %d; %d remaining", seats, id, flight.availableSeats);

        return flight;
    }
}
