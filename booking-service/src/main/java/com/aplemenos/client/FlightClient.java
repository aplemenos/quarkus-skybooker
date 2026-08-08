package com.aplemenos.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Typed MicroProfile REST Client for flight-service. The base URL is configured
 * via {@code quarkus.rest-client.flight-api.url}. Quarkus generates the HTTP
 * implementation from these JAX-RS annotations at build time.
 */
@Path("/flights")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "flight-api")
public interface FlightClient {

    /** Fetch flight details (used to snapshot the flight number and price). */
    @GET
    @Path("/{id}")
    FlightResponse getFlight(@PathParam("id") Long id);

    /** Reserve seats on the flight; returns the updated flight. */
    @POST
    @Path("/{id}/reserve")
    FlightResponse reserve(@PathParam("id") Long id, ReserveRequest request);
}
