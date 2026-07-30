-- Réglages du rapport DLC quotidien : ligne unique (id = 1), modifiable depuis l'interface.
-- Doit correspondre exactement au mapping Hibernate (schema-management.strategy=validate).
-- Pas de séquence : l'id est fixe, la ligne est insérée ici pour exister dès le premier démarrage.

create table report_settings (
    id               bigint                      not null,
    enabled          boolean                     not null,
    send_time        time(6) without time zone   not null,
    recipients       varchar(2000)               not null,
    threshold_pieces double precision            not null,
    threshold_kg     double precision            not null,
    last_sent_date   date,
    last_sent_at     timestamp(6) with time zone,
    last_status      varchar(255),
    last_error       varchar(2000),
    updated_at       timestamp(6) with time zone not null,
    primary key (id)
);

-- enabled = false : aucun envoi tant qu'aucun destinataire n'a été saisi.
insert into report_settings
    (id, enabled, send_time, recipients, threshold_pieces, threshold_kg, updated_at)
values
    (1, false, '18:00:00', '', 5, 1, now());
