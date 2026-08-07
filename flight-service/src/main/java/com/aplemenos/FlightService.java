package com.aplemenos;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Business logic for flights. A CDI {@code @ApplicationScoped} bean — a single
 * instance is shared and injected into {@code FlightResource}.
 */
@ApplicationScoped
public class FlightService {

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

        Flight flight = findById(id); // 404 if the flight does not exist

        long updated = Flight.update(
                "availableSeats = availableSeats - ?1 where id = ?2 and availableSeats >= ?1",
                seats, id);
        if (updated == 0) {
            throw new WebApplicationException(
                    "Not enough seats on flight " + id, Response.Status.CONFLICT);
        }

        // The bulk update bypassed the persistence context; reload the fresh state.
        Flight.getEntityManager().refresh(flight);

        return flight;
    }
}
