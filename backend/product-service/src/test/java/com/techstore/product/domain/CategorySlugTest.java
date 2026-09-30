package com.techstore.product.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CategorySlugTest {
    @Test
    void generatesCanonicalSlugFromCategoryName() {
        assertEquals("electronica-e-casa", CategorySlug.fromName(" Electrónica e Casa "));
    }

    @Test
    void normalizesProvidedSlugUsingSameCanonicalRule() {
        assertEquals("audio-profissional", CategorySlug.normalize(" Áudio__Profissional "));
        assertEquals("camera-4k", CategorySlug.normalize("Camera---4K"));
    }

    @Test
    void rejectsValuesThatNormalizeToAnEmptySlug() {
        assertThrows(IllegalArgumentException.class, () -> CategorySlug.fromName("--- ___ !!!"));
        assertThrows(IllegalArgumentException.class, () -> CategorySlug.normalize("   "));
    }

    @Test
    void enforcesMaximumCanonicalSlugLengthOf120Characters() {
        String maximum = "a".repeat(120);
        assertEquals(maximum, CategorySlug.normalize(maximum));
        assertThrows(IllegalArgumentException.class, () -> CategorySlug.normalize("a".repeat(121)));
    }
}
