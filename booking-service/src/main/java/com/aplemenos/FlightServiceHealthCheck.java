package com.aplemenos;

import com.aplemenos.client.FlightHealthClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * A {@code @Readiness} health check that reports whether flight-service is
 * reachable — it shows up at {@code /q/health/ready}.
 *
 * <p>Note: because booking-service <em>degrades gracefully</em> when flight-service
 * is down (bookings become PENDING), one could argue it should stay "ready" even
 * then. This check is included to demonstrate a dependency probe; in a stricter
 * design you might expose it as informational rather than failing readiness.
 */
@Readiness
@ApplicationScoped
public class FlightServiceHealthCheck implements HealthCheck {

    @Inject
    @RestClient
    FlightHealthClient flightHealthClient;

    @Override
    public HealthCheckResponse call() {
        try (Response response = flightHealthClient.live()) {
            boolean up = response.getStatus() == 200;

            return HealthCheckResponse.named("flight-service-reachable").status(up).build();
        } catch (Exception e) {
            return HealthCheckResponse.named("flight-service-reachable")
                    .down()
                    .withData("error", e.getClass().getSimpleName())
                    .build();
        }
    }
}
