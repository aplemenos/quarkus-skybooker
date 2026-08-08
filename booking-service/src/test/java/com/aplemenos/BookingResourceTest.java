package com.aplemenos;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.aplemenos.client.FlightClient;
import com.aplemenos.client.FlightResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import java.math.BigDecimal;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BookingResourceTest {

    @InjectMock
    @RestClient
    FlightClient flightClient;

    /** Stub flight-service to return flight 1 (SK101 @ 120.00) and accept the reservation. */
    private void stubFlight1() {
        FlightResponse sk101 = new FlightResponse(1L, "SK101", new BigDecimal("120.00"), 180);
        when(flightClient.getFlight(1L)).thenReturn(sk101);
        when(flightClient.reserve(eq(1L), any())).thenReturn(sk101);
    }

    @Test
    void createBooking_confirmsWithFlightInfoFromFlightService() {
        stubFlight1();

        given().contentType("application/json")
                .body("{\"flightId\":1,\"passengerName\":\"Ada Lovelace\",\"seats\":2}")
                .when().post("/bookings")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("flightNumber", is("SK101"))          // snapshotted from flight-service
                .body("seats", is(2))
                .body("totalPrice", is(240.0f))              // 120.00 * 2
                .body("status", is("CONFIRMED"));
    }

    @Test
    void createBooking_missingFlightId_returns400() {
        // Fails validation before flight-service is called.
        given().contentType("application/json")
                .body("{\"passengerName\":\"No Flight\",\"seats\":1}")
                .when().post("/bookings")
                .then().statusCode(400);
    }

    @Test
    void createBooking_zeroSeats_returns400() {
        given().contentType("application/json")
                .body("{\"flightId\":1,\"passengerName\":\"Zero Seats\",\"seats\":0}")
                .when().post("/bookings")
                .then().statusCode(400);
    }

    @Test
    void getUnknownBooking_returns404() {
        given()
                .when().get("/bookings/9999")
                .then().statusCode(404);
    }

    @Test
    void createThenGetBooking_roundTrips() {
        stubFlight1();

        Integer id = given().contentType("application/json")
                .body("{\"flightId\":1,\"passengerName\":\"Grace Hopper\",\"seats\":1}")
                .when().post("/bookings")
                .then().statusCode(201)
                .extract().path("id");

        given()
                .when().get("/bookings/" + id)
                .then().statusCode(200)
                .body("passengerName", is("Grace Hopper"))
                .body("flightNumber", is("SK101"))
                .body("status", is("CONFIRMED"));
    }
}
