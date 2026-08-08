package com.aplemenos;

import jakarta.enterprise.context.RequestScoped;
import java.util.UUID;

/**
 * A {@code @RequestScoped} bean — a fresh instance is created for each HTTP
 * request and destroyed when it ends. Holds a short id used to correlate the
 * log lines produced while handling one booking request.
 */
@RequestScoped
public class RequestContext {

    private final String requestId = UUID.randomUUID().toString().substring(0, 8);

    public String getRequestId() {
        return requestId;
    }
}
