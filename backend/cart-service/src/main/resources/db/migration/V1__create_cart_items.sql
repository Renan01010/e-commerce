CREATE TABLE cart_items (
    owner_user_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    CONSTRAINT pk_cart_items PRIMARY KEY (owner_user_id, product_id),
    CONSTRAINT ck_cart_items_quantity_positive CHECK (quantity > 0)
);