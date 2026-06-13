-- FrutApp · Drop del index muerto creado en V19.
-- listProducts hace JOIN con category y filtra por category.slug + name search,
-- no por product.category_id directo; tampoco filtra por disponible (el catalogo
-- muestra agotados en gris). findProduct usa la PK. El index de V19
-- (category_id, disponible) WHERE deleted_at IS NULL AND active = true
-- no es elegido por ninguna query del repo, solo paga el costo de mantenerlo
-- en INSERT/UPDATE de product.

DROP INDEX IF EXISTS product_active_disponible_idx;
