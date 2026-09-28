package com.techstore.product.domain;

import java.text.Normalizer;
import java.util.Locale;

public final class CategorySlug {
    public static final int MAX_LENGTH = 120;

    private CategorySlug() {}

    public static String fromName(String name) {
        return normalize(name);
    }

    public static String normalize(String value) {
        if (value == null) throw new IllegalArgumentException("Slug value is required");

        String slug = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (slug.isEmpty()) throw new IllegalArgumentException("Slug must contain letters or digits");
        if (slug.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Slug must not exceed " + MAX_LENGTH + " characters");
        }
        return slug;
    }
}
