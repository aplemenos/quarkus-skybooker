package com.aplemenos;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** Auth flow + that /bookings really requires a token. No @TestSecurity here. */
@QuarkusTest
class BookingAuthTest {

    @Test
    void login_validCredentials_returnsToken() {
        given().contentType("application/json")
                .body("{\"username\":\"demo\",\"password\":\"demo123\"}")
                .when().post("/auth/login")
                .then().statusCode(200)
                .body("token", notNullValue())
                .body("tokenType", is("Bearer"));
    }

    @Test
    void login_invalidCredentials_returns401() {
        given().contentType("application/json")
                .body("{\"username\":\"demo\",\"password\":\"wrong\"}")
                .when().post("/auth/login")
                .then().statusCode(401);
    }

    @Test
    void bookings_withoutToken_returns401() {
        given()
                .when().get("/bookings")
                .then().statusCode(401);
    }
}
