-- Reference flight data. Runs in every environment (the app has no create-flight
-- endpoint, so these flights are baseline data rather than test-only fixtures).

insert into flights (id, flightNumber, origin, destination, departureTime, totalSeats, availableSeats, price) values
 (1, 'SK101', 'ATH', 'LON', '2026-09-01 08:30:00', 180, 180, 120.00),
 (2, 'SK202', 'ATH', 'BER', '2026-09-01 12:00:00', 150, 150, 140.00),
 (3, 'SK303', 'LON', 'NYC', '2026-09-02 09:15:00', 300, 300, 480.00),
 (4, 'SK404', 'BER', 'ATH', '2026-09-03 18:45:00',   2,   2,  95.00);

-- Keep the id sequence ahead of the seeded rows.
alter sequence flights_SEQ restart with 5;
