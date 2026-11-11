# Phx Widget (Android)

Home-screen widget for Phoenix EU168: what is running NOW and what is NEXT for your ops level, the next 2 Phx
territory defences, and the next incursion and alliance tournament. Public data only, no API key.

Data: `events-data-game.json` (Phoenix-tracker, refreshed every 4 h) and the `phx_territory_schedule` table (live).

## Daily reset (the 4pm / 5pm problem)
The game resets at 09:00 Pacific = 16:00 UTC in summer, 17:00 UTC in winter. Events published before a clock change keep
the old hour. Events starting or ending at exactly 16:00 or 17:00 UTC are moved onto the real reset so everything lines up.
Incursions and tournaments are not moved: they run on their own regional times.

## Build
Push to `main`: GitHub Actions runs the unit tests, builds the APK and publishes it as a release (same as the bus widget).
Install the APK, then long-press the home screen > Widgets > Phx Widget. Pick your ops level on the settings screen.
