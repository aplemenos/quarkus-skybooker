package com.aplemenos;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

/**
 * CDI observers. {@code @Observes} lets a method react to events without the
 * producer knowing about it (loose coupling).
 */
@ApplicationScoped
public class BookingEventListener {

    private static final Logger LOG = Logger.getLogger(BookingEventListener.class);

    /** Reacts to the built-in Quarkus startup event. */
    void onStart(@Observes StartupEvent event) {
        LOG.info("booking-service started and ready to take bookings");
    }

    /**
     * Reacts to our custom {@link BookingCreatedEvent}. In a real system this is
     * where you might publish to Kafka, send a confirmation email, etc.
     */
    void onBookingCreated(@Observes BookingCreatedEvent event) {
        LOG.infof("Event: booking %d for flight %d (%d seat(s)) is %s",
                event.bookingId(), event.flightId(), event.seats(), event.status());
    }
}
