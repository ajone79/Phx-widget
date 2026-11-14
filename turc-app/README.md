# Turc Crossovers (Android)

Reads the public Phoenix event feed (`events-data-game.json`) and shows events you can score in with the same action
at the same time, so you can stack your effort. Pick your grade, 3 / 7 / 14 days, optionally "3+ only".

It works out what each event scores on from its title and description (kill Polaron FKR hostiles, upgrade buildings with
G3+ materials, etc). Passes and metas are ignored. It is a guide, not the game's own rules: check the in-game event
details before committing resources.

Build: pushing to `main` runs `.github/workflows/build-turc-apk.yml`, which tests, builds and publishes
`TurcCrossovers.apk` as a release tagged `turc-N` (not marked "latest", so it never replaces the Phx Widget release).
