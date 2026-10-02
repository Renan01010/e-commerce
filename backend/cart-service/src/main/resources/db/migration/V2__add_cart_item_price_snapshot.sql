ALTER TABLE cart_items
    ADD COLUMN unit_price NUMERIC(12, 2),
    ADD COLUMN price_snapshot_status VARCHAR(16) NOT NULL DEFAULT 'PENDING';

ALTER TABLE cart_items
    ALTER COLUMN price_snapshot_status DROP DEFAULT,
    ADD CONSTRAINT ck_cart_items_price_snapshot_status
        CHECK (price_snapshot_status IN ('PENDING', 'KNOWN', 'UNKNOWN')),
    ADD CONSTRAINT ck_cart_items_price_snapshot_consistency
        CHECK (
            (price_snapshot_status = 'KNOWN' AND unit_price IS NOT NULL)
            OR (price_snapshot_status IN ('PENDING', 'UNKNOWN') AND unit_price IS NULL)
        ),
    ADD CONSTRAINT ck_cart_items_unit_price_non_negative
        CHECK (unit_price IS NULL OR unit_price >= 0);
