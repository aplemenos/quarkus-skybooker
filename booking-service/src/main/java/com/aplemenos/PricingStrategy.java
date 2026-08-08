package com.aplemenos;

import java.math.BigDecimal;

/** Strategy for computing a booking's total price. */
public interface PricingStrategy {

    BigDecimal totalPrice(BigDecimal unitPrice, int seats);
}
