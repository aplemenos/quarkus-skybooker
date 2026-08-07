package com.aplemenos;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BookingResourceTest {

    @Test
    void createBooking_returnsPendingBooking() {
        given().contentType("application/json")
                .body("{\"flightId\":1,\"passengerName\":\"Ada Lovelace\",\"seats\":2}")
                .when().post("/bookings")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("flightId", is(1))
                .body("passengerName", is("Ada Lovelace"))
                .body("seats", is(2))
                .body("status", is("PENDING"));
    }

    @Test
    void createBooking_missingFlightId_returns400() {
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
        Integer id = given().contentType("application/json")
                .body("{\"flightId\":2,\"passengerName\":\"Grace Hopper\",\"seats\":1}")
                .when().post("/bookings")
                .then().statusCode(201)
                .extract().path("id");

        given()
                .when().get("/bookings/" + id)
                .then().statusCode(200)
                .body("passengerName", is("Grace Hopper"))
                .body("flightId", is(2));
    }
}
