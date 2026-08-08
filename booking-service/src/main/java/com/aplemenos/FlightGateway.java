package com.aplemenos;

import com.aplemenos.client.FlightClient;
import com.aplemenos.client.FlightResponse;
import com.aplemenos.client.ReserveRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Resilient wrapper around the flight-service calls. Fault-tolerance annotations
 * live here (a separate bean) because MicroProfile Fault Tolerance uses CDI
 * interceptors, which — like {@code @Transactional} — do NOT fire on same-bean
 * self-invocation. {@code BookingService} injects and calls this bean, so the
 * interceptors apply.
 */
@ApplicationScoped
public class FlightGateway {

    private static final Logger LOG = Logger.getLogger(FlightGateway.class);

    @Inject
    @RestClient
    FlightClient flightClient;

    /**
     * Fetches the flight and reserves seats, with resilience:
     * <ul>
     *   <li>{@code @Timeout} — fail fast if flight-service is too slow (2s).</li>
     *   <li>{@code @Retry} — retry transient/network failures a few times, but
     *       {@code abortOn} business errors (404/409) so we don't hammer a
     *       deterministic failure.</li>
     *   <li>{@code @Fallback} — if flight-service is still unreachable, degrade to
     *       a "not confirmed" result; {@code skipOn} the business errors so they
     *       propagate to the caller for proper 404/409 translation.</li>
     * </ul>
     */
    @Timeout(2000)
    @Retry(maxRetries = 5, delay = 500, abortOn = WebApplicationException.class)
    @Fallback(fallbackMethod = "flightUnavailable", skipOn = WebApplicationException.class)
    public FlightReservation reserveSeats(Long flightId, int seats) {
        LOG.debugf("Calling flight-service to reserve %d seat(s) on flight %d", seats, flightId);

        FlightResponse flight = flightClient.getFlight(flightId);
        flightClient.reserve(flightId, new ReserveRequest(seats));

        return FlightReservation.confirmed(flight.flightNumber(), flight.price());
    }

    /** Runs when flight-service is unreachable/timed out after all retries. */
    @SuppressWarnings("unused")
    FlightReservation flightUnavailable(Long flightId, int seats) {
        LOG.warnf("flight-service unavailable after retries; booking %d seat(s) on flight %d will be PENDING",
                seats, flightId);

        return FlightReservation.unavailable();
    }
}
