package com.aplemenos;

import static io.restassured.RestAssured.given;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URL;
import org.junit.jupiter.api.Test;

/** No @TestSecurity here — verifies the endpoints really require authentication. */
@QuarkusTest
class FlightSecurityTest {

    // management=true already points at the management interface's /q root.
    @TestHTTPResource(value = "/health/live", management = true)
    URL livenessUrl;

    @Test
    void flights_withoutToken_returns401() {
        given()
                .when().get("/flights")
                .then().statusCode(401);
    }

    @Test
    void health_isPublic_evenWithoutToken() {
        given()
                .when().get(livenessUrl.toExternalForm())
                .then().statusCode(200);
    }
}
