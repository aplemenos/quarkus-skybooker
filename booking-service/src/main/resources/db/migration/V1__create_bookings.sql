-- Schema for bookings. Owned by Flyway and applied in EVERY environment so
-- dev/test/prod stay identical; Hibernate only validates against it.
-- Mirrors the DDL Hibernate generates for the Booking entity.

create sequence bookings_SEQ start with 1 increment by 50;

create table bookings (
    id            bigint       not null,
    flightId      bigint,
    flightNumber  varchar(255),
    passengerName varchar(255),
    seats         integer      not null,
    totalPrice    numeric(38, 2),
    status        varchar(255) check (status in ('CONFIRMED', 'PENDING', 'REJECTED')),
    createdAt     timestamp(6) with time zone,
    primary key (id)
);
