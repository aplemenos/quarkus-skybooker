package com.aplemenos;

import com.aplemenos.dto.AvailabilityResponse;
import com.aplemenos.dto.FlightDto;
import com.aplemenos.dto.ReserveRequest;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * JAX-RS resource exposing the flight catalog and seat operations.
 */
@Path("/flights")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class FlightResource {

    @Inject
    FlightService flightService;

    @GET
    public List<FlightDto> list() {
        return flightService.listAll().stream().map(FlightDto::from).toList();
    }

    @GET
    @Path("/{id}")
    public FlightDto get(@PathParam("id") Long id) {
        return FlightDto.from(flightService.findById(id));
    }

    /** Used by booking-service to check availability before booking. */
    @GET
    @Path("/{id}/availability")
    public AvailabilityResponse availability(@PathParam("id") Long id,
                                             @QueryParam("seats") @DefaultValue("1") int seats) {
        Flight flight = flightService.findById(id);
        return new AvailabilityResponse(id, seats, flight.availableSeats >= seats, flight.availableSeats);
    }

    /** Atomically reserves seats; used by booking-service when confirming a booking. */
    @POST
    @Path("/{id}/reserve")
    public FlightDto reserve(@PathParam("id") Long id, ReserveRequest request) {
        return FlightDto.from(flightService.reserve(id, request.seats()));
    }
}
