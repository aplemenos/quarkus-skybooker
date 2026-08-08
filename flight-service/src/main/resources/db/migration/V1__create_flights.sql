-- Schema for flights. Owned by Flyway and applied in EVERY environment
-- (dev/test/prod) so they stay identical; Hibernate only validates against it.
-- The DDL mirrors what Hibernate generates for the Flight entity.

create sequence flights_SEQ start with 1 increment by 50;

create table flights (
    id             bigint        not null,
    flightNumber   varchar(255),
    origin         varchar(255),
    destination    varchar(255),
    departureTime  timestamp(6),
    totalSeats     integer       not null,
    availableSeats integer       not null,
    price          numeric(38, 2),
    primary key (id)
);
