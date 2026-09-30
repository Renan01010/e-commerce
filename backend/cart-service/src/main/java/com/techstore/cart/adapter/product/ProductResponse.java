package com.techstore.cart.adapter.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
record ProductResponse(UUID id, String name, BigDecimal price, String brand, int quantity,
					   String imageUrl, boolean isActive) {}