package com.aplemenos.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

/**
 * Propagates the caller's Bearer token onto the outbound call to flight-service,
 * so the internal hop is authenticated too (zero trust). The incoming headers
 * come from the current inbound request; the token is copied through to the
 * REST client request.
 */
@ApplicationScoped
public class AuthHeadersFactory implements ClientHeadersFactory {

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                                                  MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        String authorization = incomingHeaders.getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null) {
            result.add(HttpHeaders.AUTHORIZATION, authorization);
        }
        return result;
    }
}
