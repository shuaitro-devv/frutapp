-- Catalog expansion: 2 nuevas categorías (Hierbas, Despensa) + 21 productos nuevos.
-- Cilantro se reclasifica de Verduras a Hierbas (estaba mal categorizado en el seed V1).

-- ============================================================
-- Nuevas categorías
-- ============================================================
INSERT INTO category (id, name, slug, sort_order) VALUES
    ('33333333-3333-3333-3333-333333333333', 'Hierbas',  'hierbas',  3),
    ('44444444-4444-4444-4444-444444444444', 'Despensa', 'despensa', 4);

-- ============================================================
-- Cilantro: Verduras → Hierbas (bug de categorización del V1)
-- ============================================================
UPDATE product
SET category_id = '33333333-3333-3333-3333-333333333333',
    updated_at  = now()
WHERE slug = 'cilantro';

-- ============================================================
-- Productos nuevos
-- image_key debe matchear el nombre del drawable empaquetado en la app
-- (app/src/commonMain/composeResources/drawable/<image_key>.png).
-- ============================================================
INSERT INTO product (category_id, name, slug, description, price_clp, unit, image_key) VALUES
    -- Frutas (10 nuevas)
    ('11111111-1111-1111-1111-111111111111', 'Frutilla',       'frutilla',       'Frutilla dulce de temporada.',                2990, 'kg',          'frutillas'),
    ('11111111-1111-1111-1111-111111111111', 'Kiwi',           'kiwi',           'Kiwi rico en vitamina C.',                    2490, 'kg',          'kiwi'),
    ('11111111-1111-1111-1111-111111111111', 'Mango',          'mango',          'Mango maduro listo para comer.',              2990, 'unidad',      'mango'),
    ('11111111-1111-1111-1111-111111111111', 'Mandarina',      'mandarina',      'Mandarina jugosa y fácil de pelar.',          1690, 'kg',          'mandarinas'),
    ('11111111-1111-1111-1111-111111111111', 'Manzana verde',  'manzana-verde',  'Manzana verde ácida y crujiente.',            1890, 'kg',          'manzana_verde'),
    ('11111111-1111-1111-1111-111111111111', 'Melón',          'melon',          'Melón aromático de pulpa dulce.',             1990, 'unidad',      'melon'),
    ('11111111-1111-1111-1111-111111111111', 'Pera',           'pera',           'Pera dulce y jugosa.',                        2290, 'kg',          'pera'),
    ('11111111-1111-1111-1111-111111111111', 'Piña',           'pina',           'Piña tropical para jugo o postre.',           2490, 'unidad',      'pina'),
    ('11111111-1111-1111-1111-111111111111', 'Sandía',         'sandia',         'Sandía refrescante para el calor.',           3990, 'unidad',      'sandia'),
    ('11111111-1111-1111-1111-111111111111', 'Uvas',           'uvas',           'Uvas dulces de la temporada.',                2990, 'kg',          'uvas'),

    -- Verduras (4 nuevas)
    ('22222222-2222-2222-2222-222222222222', 'Brócoli',           'brocoli',           'Brócoli fresco rico en hierro.',     1490, 'unidad',  'brocoli'),
    ('22222222-2222-2222-2222-222222222222', 'Choclo',            'choclo',            'Choclo dulce de la temporada.',       890, 'unidad',  'choclo'),
    ('22222222-2222-2222-2222-222222222222', 'Coliflor',          'coliflor',          'Coliflor blanca y crujiente.',       1490, 'unidad',  'coliflor'),
    ('22222222-2222-2222-2222-222222222222', 'Zapallo italiano',  'zapallo-italiano',  'Zapallo italiano versátil.',         1290, 'unidad',  'zapallo_italiano'),

    -- Hierbas (5 nuevas; Cilantro ya existe y se movió arriba)
    ('33333333-3333-3333-3333-333333333333', 'Albahaca', 'albahaca', 'Albahaca fresca aromática.',     700, 'atado', 'albahaca'),
    ('33333333-3333-3333-3333-333333333333', 'Menta',    'menta',    'Menta para mojitos o infusión.', 600, 'atado', 'menta'),
    ('33333333-3333-3333-3333-333333333333', 'Orégano',  'oregano',  'Orégano fresco recién cortado.', 700, 'atado', 'oregano'),
    ('33333333-3333-3333-3333-333333333333', 'Perejil',  'perejil',  'Perejil fresco para aliños.',    500, 'atado', 'perejil'),
    ('33333333-3333-3333-3333-333333333333', 'Romero',   'romero',   'Romero aromático para asados.',  600, 'atado', 'romero'),

    -- Despensa (3 nuevas)
    ('44444444-4444-4444-4444-444444444444', 'Huevos de campo', 'huevos',   'Huevos de gallinas libres, color de cáscara natural.', 4990, 'docena',       'huevos'),
    ('44444444-4444-4444-4444-444444444444', 'Jengibre',        'jengibre', 'Jengibre fresco para té y cocina.',                    3990, 'kg',           'jengibre'),
    ('44444444-4444-4444-4444-444444444444', 'Miel',            'miel',     'Miel de abeja natural de productor chileno.',          7990, 'frasco 500g',  'miel');
