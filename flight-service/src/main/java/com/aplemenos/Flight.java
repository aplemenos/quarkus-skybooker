package com.aplemenos;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A scheduled flight and its remaining seats.
 *
 * <p>Uses the Panache active-record style: public fields (Panache generates the
 * accessors) and static query methods inherited from {@link PanacheEntity},
 * which also supplies the {@code Long id}.
 */
@Entity
@Table(name = "flights")
public class Flight extends PanacheEntity {

    public String flightNumber;
    public String origin;
    public String destination;
    public LocalDateTime departureTime;
    public int totalSeats;
    public int availableSeats;
    public BigDecimal price;
}
