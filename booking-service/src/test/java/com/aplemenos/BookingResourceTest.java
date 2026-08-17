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
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import java.net.URL;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import java.math.BigDecimal;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestSecurity(user = "tester")   // /bookings are @Authenticated; run tests as a logged-in user
class BookingResourceTest {

    @InjectMock
    @RestClient
    FlightClient flightClient;

    // Health and metrics live on the management port, not the app port.
    // management=true resolves to the management interface base (which already
    // includes its /q root path), so these values are relative to that.
    @TestHTTPResource(value = "/health/live", management = true)
    URL livenessUrl;

    @TestHTTPResource(value = "/metrics", management = true)
    URL metricsUrl;

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
    void createBooking_exceedsConfiguredMaxSeats_returns400() {
        // booking.max-seats=9; requesting 20 is rejected before flight-service is called.
        given().contentType("application/json")
                .body("{\"flightId\":1,\"passengerName\":\"Too Many Seats\",\"seats\":20}")
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
    void createBooking_flightServiceUnreachable_degradesToPending() {
        // Simulate flight-service being down: every call fails with a network error.
        // @Retry exhausts, then @Fallback runs -> booking is accepted as PENDING.
        when(flightClient.getFlight(1L))
                .thenThrow(new ProcessingException("Connection refused"));

        given().contentType("application/json")
                .body("{\"flightId\":1,\"passengerName\":\"Resilient Rita\",\"seats\":2}")
                .when().post("/bookings")
                .then().statusCode(201)
                .body("status", is("PENDING"))          // degraded, not failed
                .body("flightNumber", is((String) null));
    }

    @Test
    void createBooking_flightNotFound_propagates404NotFallback() {
        // A 404 is a business error: @Retry aborts and @Fallback is skipped, so it
        // must surface as 404 — NOT be swallowed into a PENDING booking.
        when(flightClient.getFlight(1L))
                .thenThrow(new NotFoundException("Flight 1 not found"));

        given().contentType("application/json")
                .body("{\"flightId\":1,\"passengerName\":\"Ghost\",\"seats\":1}")
                .when().post("/bookings")
                .then().statusCode(404);
    }

    @Test
    void health_liveness_isUp() {
        // Liveness only (readiness would ping flight-service, which isn't running in tests).
        given()
                .when().get(livenessUrl.toExternalForm())
                .then().statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void metrics_endpoint_isExposed() {
        given()
                .when().get(metricsUrl.toExternalForm())
                .then().statusCode(200);
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
