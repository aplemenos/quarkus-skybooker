package com.aplemenos.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Tiny REST client to probe flight-service's liveness endpoint. Uses the
 * {@code flight-health-api} base URL, which points at flight-service's
 * MANAGEMENT port (where health lives), not its app port.
 */
@RegisterRestClient(configKey = "flight-health-api")
public interface FlightHealthClient {

    @GET
    @Path("/q/health/live")
    Response live();
}
