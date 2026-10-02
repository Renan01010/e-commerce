package com.techstore.cart.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record UnitPriceSnapshot(Status status, BigDecimal amount) {
    public UnitPriceSnapshot {
        if (status == null) throw new IllegalArgumentException("Price snapshot status is required");
        if (status == Status.KNOWN) {
            if (amount == null) throw new IllegalArgumentException("Known price is required");
            if (amount.signum() < 0) throw new IllegalArgumentException("Price cannot be negative");
            amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        } else if (amount != null) {
            throw new IllegalArgumentException("Unknown price cannot have an amount");
        }
    }

    public static UnitPriceSnapshot known(BigDecimal amount) {
        return new UnitPriceSnapshot(Status.KNOWN, amount);
    }

    public static UnitPriceSnapshot unknown() {
        return new UnitPriceSnapshot(Status.UNKNOWN, null);
    }

    public enum Status {
        KNOWN,
        UNKNOWN
    }
}
