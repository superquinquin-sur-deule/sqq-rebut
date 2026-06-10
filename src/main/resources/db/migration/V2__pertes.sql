-- Mode « Pertes » : discriminateur de ligne + motif de rupture (Odoo stock.scrap.origin).
-- Doit correspondre exactement au mapping Hibernate (schema-management.strategy=validate).
-- 'DLC' par défaut pour rétro-remplir les lignes existantes ; Panache fixe toujours `type` à l'insert.

alter table releve_line add column type varchar(16) not null default 'DLC';
alter table releve_line alter column dlc drop not null;
alter table releve_line add column motif_id    bigint;
alter table releve_line add column motif_label varchar(255);
