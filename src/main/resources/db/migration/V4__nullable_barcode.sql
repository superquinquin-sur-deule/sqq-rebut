-- Produits sans code-barres (salades / F&L au poids) : identifiés par product_id Odoo.
-- Doit rester synchrone avec le mapping Hibernate (schema-management.strategy=validate).
alter table releve_line alter column barcode drop not null;
