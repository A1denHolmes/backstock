CREATE SEQUENCE categories_seq INCREMENT BY 50;

CREATE TABLE categories
(
    id         bigint PRIMARY KEY    DEFAULT nextval('categories_seq'),
    name       varchar(100) NOT NULL,
    slug       varchar(100) NOT NULL UNIQUE,
    is_active  boolean      NOT NULL DEFAULT true,
    created_at timestamptz  NOT NULL,
    updated_at timestamptz  NOT NULL
);

ALTER SEQUENCE categories_seq OWNED BY categories.id;
