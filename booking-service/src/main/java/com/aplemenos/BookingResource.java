package com.aplemenos;

import com.aplemenos.dto.BookingDto;
import com.aplemenos.dto.CreateBookingRequest;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class BookingResource {

    @Inject
    BookingService bookingService;

    @GET
    public List<BookingDto> list() {
        return bookingService.listAll().stream().map(BookingDto::from).toList();
    }

    @GET
    @Path("/{id}")
    public BookingDto get(@PathParam("id") Long id) {
        return BookingDto.from(bookingService.findById(id));
    }

    @POST
    public Response create(CreateBookingRequest request) {
        BookingDto created = BookingDto.from(bookingService.create(request));
        return Response.created(URI.create("/bookings/" + created.id())).entity(created).build();
    }
}
