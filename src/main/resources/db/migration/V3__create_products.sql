CREATE SEQUENCE products_seq INCREMENT BY 50;

CREATE TABLE products
(
    id             bigint PRIMARY KEY      DEFAULT nextval('products_seq'),
    sku            varchar(64)    NOT NULL UNIQUE,
    name           varchar(200)   NOT NULL,
    description    text,
    price          numeric(19, 4) NOT NULL CHECK (price > 0),
    stock_quantity int            NOT NULL CHECK (stock_quantity >= 0),
    category_id    bigint         NOT NULL REFERENCES categories (id),
    image_url      varchar(2048),
    is_active      boolean        NOT NULL DEFAULT true,
    version        bigint         NOT NULL DEFAULT 0,
    created_at     timestamptz    NOT NULL,
    updated_at     timestamptz    NOT NULL
);

CREATE INDEX products_category_id_idx ON products (category_id);

ALTER SEQUENCE products_seq OWNED BY products.id;
