-- FrutApp · Migración inicial (Sprint 1: auth + catálogo)
-- gen_random_uuid() viene en pgcrypto (built-in en PG13+; la extensión lo asegura).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- Usuarios y autenticación
-- ============================================================
CREATE TABLE app_user (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          TEXT        NOT NULL,
    email         TEXT        NOT NULL,
    phone         TEXT,
    password_hash TEXT        NOT NULL,
    role          TEXT        NOT NULL DEFAULT 'CUSTOMER',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ
);

-- Email único solo entre usuarios no borrados (soft delete).
CREATE UNIQUE INDEX ux_app_user_email_active
    ON app_user (lower(email)) WHERE deleted_at IS NULL;

CREATE TABLE refresh_token (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash  TEXT        NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_refresh_token_user ON refresh_token (user_id);
CREATE UNIQUE INDEX ux_refresh_token_hash ON refresh_token (token_hash);

-- ============================================================
-- Catálogo
-- ============================================================
CREATE TABLE category (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       TEXT NOT NULL,
    slug       TEXT NOT NULL UNIQUE,
    sort_order INT  NOT NULL DEFAULT 0
);

CREATE TABLE product (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID        NOT NULL REFERENCES category (id),
    name        TEXT        NOT NULL,
    slug        TEXT        NOT NULL UNIQUE,
    description TEXT        NOT NULL DEFAULT '',
    price_clp   INT         NOT NULL,
    unit        TEXT        NOT NULL DEFAULT 'kg',
    image_key   TEXT        NOT NULL,
    active      BOOLEAN     NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX ix_product_category ON product (category_id) WHERE deleted_at IS NULL;

-- ============================================================
-- Seed de catálogo (mapeado a las fotos en composeResources/drawable)
-- image_key = nombre del drawable en la app.
-- ============================================================
INSERT INTO category (id, name, slug, sort_order) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Frutas',   'frutas',   1),
    ('22222222-2222-2222-2222-222222222222', 'Verduras', 'verduras', 2);

INSERT INTO product (category_id, name, slug, description, price_clp, unit, image_key) VALUES
    -- Frutas
    ('11111111-1111-1111-1111-111111111111', 'Manzana roja', 'manzana-roja', 'Manzana roja fresca y crujiente.',     1690, 'kg',      'manzana_roja'),
    ('11111111-1111-1111-1111-111111111111', 'Naranja',      'naranja',      'Naranja jugosa de temporada.',          1290, 'kg',      'naranja'),
    ('11111111-1111-1111-1111-111111111111', 'Limón',        'limon',        'Limón ácido ideal para aliños.',        1990, 'kg',      'limon'),
    ('11111111-1111-1111-1111-111111111111', 'Plátano',      'platano',      'Plátano dulce y energético.',           1490, 'kg',      'platano'),
    ('11111111-1111-1111-1111-111111111111', 'Palta Hass',   'palta-hass',   'Palta Hass cremosa, lista para untar.', 4990, 'kg',      'palta_hass'),
    -- Verduras
    ('22222222-2222-2222-2222-222222222222', 'Tomate',          'tomate',          'Tomate fresco de feria.',             1390, 'kg',      'tomate'),
    ('22222222-2222-2222-2222-222222222222', 'Lechuga',         'lechuga',         'Lechuga fresca hoja verde.',           990, 'unidad',  'lechuga'),
    ('22222222-2222-2222-2222-222222222222', 'Papa',            'papa',            'Papa para todo tipo de preparación.',  990, 'kg',      'papa'),
    ('22222222-2222-2222-2222-222222222222', 'Cebolla',         'cebolla',         'Cebolla de guarda.',                   890, 'kg',      'cebolla'),
    ('22222222-2222-2222-2222-222222222222', 'Zanahoria',       'zanahoria',       'Zanahoria fresca y dulce.',            790, 'kg',      'zanahoria'),
    ('22222222-2222-2222-2222-222222222222', 'Pepino',          'pepino',          'Pepino ensalada.',                    1190, 'kg',      'pepino'),
    ('22222222-2222-2222-2222-222222222222', 'Pimentón verde',  'pimenton-verde',  'Pimentón verde fresco.',              1990, 'kg',      'pimenton_verde'),
    ('22222222-2222-2222-2222-222222222222', 'Pimiento rojo',   'pimiento-rojo',   'Pimiento rojo dulce.',                2490, 'kg',      'pimiento_rojo'),
    ('22222222-2222-2222-2222-222222222222', 'Ajo',             'ajo',             'Ajo chileno aromático.',              4990, 'kg',      'ajo'),
    ('22222222-2222-2222-2222-222222222222', 'Cilantro',        'cilantro',        'Cilantro fresco en atado.',            590, 'unidad',  'cilantro');
