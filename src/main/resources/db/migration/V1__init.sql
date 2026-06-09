-- Schéma initial du relevé DLC.
-- Doit correspondre exactement au mapping Hibernate (schema-management.strategy=validate).
-- Séquences Panache : @GeneratedValue par défaut → une séquence par entité, incrément 50.

create sequence releve_seq start with 1 increment by 50;
create sequence releve_line_seq start with 1 increment by 50;

create table releve (
    id          bigint not null,
    releve_date date   not null,
    primary key (id),
    constraint uq_releve_date unique (releve_date)
);

create table releve_line (
    id         bigint                      not null,
    releve_id  bigint                      not null,
    product_id bigint,
    barcode    varchar(255)                not null,
    name       varchar(255)                not null,
    rayon      varchar(255),
    uom        varchar(255),
    uom_id     bigint,
    dlc        date                        not null,
    qty        integer                     not null,
    sent       boolean                     not null,
    scrap_ref  varchar(255),
    created_at timestamp(6) with time zone not null,
    primary key (id),
    constraint fk_releve_line_releve foreign key (releve_id) references releve (id)
);
