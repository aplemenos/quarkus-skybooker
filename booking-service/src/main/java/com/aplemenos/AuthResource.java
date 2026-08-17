package com.aplemenos;

import com.aplemenos.dto.LoginRequest;
import com.aplemenos.dto.TokenResponse;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Issues JWTs. Public (no {@code @Authenticated}) — this is where you get a token.
 * Validates credentials, then signs a JWT with the private key.
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);
    private static final long TOKEN_TTL_SECONDS = 3600;

    @Inject
    @ConfigProperty(name = "app.auth.username")
    String configuredUsername;

    @Inject
    @ConfigProperty(name = "app.auth.password")
    String configuredPassword;

    @Inject
    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @POST
    @Path("/login")
    public TokenResponse login(LoginRequest request) {
        if (request == null
                || !configuredUsername.equals(request.username())
                || !configuredPassword.equals(request.password())) {
            LOG.warnf("Failed login attempt for user '%s'",
                    request == null ? null : request.username());
            throw new WebApplicationException("Invalid credentials", Response.Status.UNAUTHORIZED);
        }

        String token = Jwt.issuer(issuer)
                .upn(request.username())              // the principal name
                .expiresIn(Duration.ofSeconds(TOKEN_TTL_SECONDS))
                .sign();                              // signs with smallrye.jwt.sign.key.location

        LOG.infof("Issued token for user '%s'", request.username());
        return TokenResponse.bearer(token, TOKEN_TTL_SECONDS);
    }
}
