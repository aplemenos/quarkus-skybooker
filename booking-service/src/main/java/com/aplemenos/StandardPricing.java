package com.aplemenos;

import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;

/** Standard pricing: unit price × seats. Qualified with {@link Standard}. */
@ApplicationScoped
@Standard
public class StandardPricing implements PricingStrategy {

    @Override
    public BigDecimal totalPrice(BigDecimal unitPrice, int seats) {
        return unitPrice.multiply(BigDecimal.valueOf(seats));
    }
}
