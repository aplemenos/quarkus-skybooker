package com.aplemenos;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import java.net.URL;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestSecurity(user = "tester")   // endpoints are @Authenticated; run tests as a logged-in user
class FlightResourceTest {

    // Health and metrics live on the management port, not the app port.
    // management=true resolves to the management interface base (which already
    // includes its /q root path), so these values are relative to that.
    @TestHTTPResource(value = "/health/live", management = true)
    URL livenessUrl;

    @TestHTTPResource(value = "/metrics", management = true)
    URL metricsUrl;

    @Test
    void listFlights_returnsSeededData() {
        given()
                .when().get("/flights")
                .then().statusCode(200)
                .body("size()", is(4));
    }

    @Test
    void getFlight_returnsFlight() {
        given()
                .when().get("/flights/1")
                .then().statusCode(200)
                .body("flightNumber", is("SK101"))
                .body("availableSeats", is(180));
    }

    @Test
    void getUnknownFlight_returns404() {
        given()
                .when().get("/flights/9999")
                .then().statusCode(404);
    }

    @Test
    void availability_reportsSeats() {
        given().queryParam("seats", 2)
                .when().get("/flights/4/availability")
                .then().statusCode(200)
                .body("available", is(true))
                .body("availableSeats", is(2));
    }

    @Test
    void reserve_decrementsSeats() {
        // flight 2 is only touched by this test, so the assertion is order-independent
        given().contentType("application/json").body("{\"seats\":1}")
                .when().post("/flights/2/reserve")
                .then().statusCode(200)
                .body("availableSeats", is(149));
    }

    @Test
    void reserve_tooManySeats_conflict() {
        // flight 4 has only 2 seats; requesting 5 fails and does not change state
        given().contentType("application/json").body("{\"seats\":5}")
                .when().post("/flights/4/reserve")
                .then().statusCode(409);
    }

    @Test
    void reserve_exceedsConfiguredMaxPerReservation_returns400() {
        // flight.max-seats-per-reservation=10; requesting 11 is rejected up front.
        given().contentType("application/json").body("{\"seats\":11}")
                .when().post("/flights/1/reserve")
                .then().statusCode(400);
    }

    @Test
    void health_liveness_isUp() {
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
}
