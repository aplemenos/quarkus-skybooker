package com.aplemenos.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Tiny REST client to probe flight-service's liveness endpoint. Reuses the
 * {@code flight-api} base URL, so it points at the same flight-service.
 */
@RegisterRestClient(configKey = "flight-api")
public interface FlightHealthClient {

    @GET
    @Path("/q/health/live")
    Response live();
}
