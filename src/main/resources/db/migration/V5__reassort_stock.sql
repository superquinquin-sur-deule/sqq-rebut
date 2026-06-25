-- Snapshot du stock Odoo (qty_available) au moment du scan, affiché sur les lignes réassort.
-- Nullable : les lignes existantes restent vides.
alter table releve_line add column qty_available double precision;
