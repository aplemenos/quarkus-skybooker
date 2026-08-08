package com.aplemenos;

import com.aplemenos.dto.CreateBookingRequest;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Business logic for bookings. Calls flight-service through {@link FlightGateway}
 * (which adds fault tolerance) to reserve seats, then persists the booking.
 */
@ApplicationScoped
public class BookingService {

    private static final Logger LOG = Logger.getLogger(BookingService.class);

    @Inject
    FlightGateway flightGateway;

    /** Max seats per booking — from MicroProfile Config (booking.max-seats). */
    @Inject
    @ConfigProperty(name = "booking.max-seats")
    int maxSeats;

    /** Pricing strategy selected via the @Standard CDI qualifier. */
    @Inject
    @Standard
    PricingStrategy pricingStrategy;

    /** A fresh instance per HTTP request, used to correlate logs. */
    @Inject
    RequestContext requestContext;

    /** Fires a CDI event after a booking is created (observed by BookingEventListener). */
    @Inject
    Event<BookingCreatedEvent> bookingCreatedEvent;

    /** Micrometer registry — exposes custom metrics at /q/metrics (Prometheus). */
    @Inject
    MeterRegistry meterRegistry;

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
            LOG.warn("flightId is null");

            throw new WebApplicationException("flightId is required", Response.Status.BAD_REQUEST);
        }

        if (request.seats() < 1) {
            LOG.warn("seats is less than 1");

            throw new WebApplicationException("seats must be at least 1", Response.Status.BAD_REQUEST);
        }

        if (request.seats() > maxSeats) {
            throw new WebApplicationException(
                    "Cannot book more than " + maxSeats + " seats per booking",
                    Response.Status.BAD_REQUEST);
        }

        LOG.infof("[%s] Creating booking: flight=%d seats=%d passenger=%s",
                requestContext.getRequestId(), request.flightId(), request.seats(),
                request.passengerName());

        // Reserve via flight-service. Business errors (404/409) skip the fallback
        // and surface here for translation; an unreachable flight-service instead
        // returns an unconfirmed reservation (the fallback ran).
        FlightReservation reservation;
        try {
            reservation = flightGateway.reserveSeats(request.flightId(), request.seats());
        } catch (WebApplicationException e) {
            int status = e.getResponse() != null ? e.getResponse().getStatus() : 500;
            if (status == 404) {
                throw new NotFoundException("Flight " + request.flightId() + " not found");
            }

            if (status == 409) {
                LOG.warn("flight has no available seats");

                throw new WebApplicationException(
                        "Flight " + request.flightId() + " has no available seats for "
                                + request.seats() + " passenger(s)", Response.Status.CONFLICT);
            }

            throw e;
        }

        Booking booking = new Booking();
        booking.flightId = request.flightId();
        booking.passengerName = request.passengerName();
        booking.seats = request.seats();

        if (reservation.confirmed()) {
            booking.flightNumber = reservation.flightNumber();
            booking.totalPrice = pricingStrategy.totalPrice(reservation.price(), request.seats());
            booking.status = BookingStatus.CONFIRMED;
        } else {
            // Degraded path: flight-service was unreachable. Accept the booking as
            // PENDING and reconcile later, rather than failing the customer.
            booking.status = BookingStatus.PENDING;
        }

        booking.persist();
        LOG.infof("[%s] Booking %d %s (flight=%s, total=%s)",
                requestContext.getRequestId(), booking.id, booking.status,
                booking.flightNumber, booking.totalPrice);

        // Custom metric: count bookings by status (bookings_created_total{status="..."}).
        meterRegistry.counter("bookings.created", "status", booking.status.name()).increment();

        // Fire a CDI event — observers react without BookingService knowing about them.
        bookingCreatedEvent.fire(new BookingCreatedEvent(
                booking.id, booking.flightId, booking.seats, booking.status));

        return booking;
    }
}
