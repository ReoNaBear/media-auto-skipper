## Purpose

Show the user which tracks the app skipped and why, so rules can be checked and tuned. Data stays on the device.

## ADDED Requirements

### Requirement: Local skip history storage

The app SHALL store skip records locally with the fields time, package name, title, artist and match reason, and SHALL NOT send them off the device. The app SHALL keep at most the newest 1000 records and delete older ones.

#### Scenario: Records persist

- **WHEN** a skip is recorded and the app is restarted
- **THEN** the record is still in the history

#### Scenario: Retention limit

- **WHEN** the 1001st record is written
- **THEN** the oldest record is deleted and 1000 records remain

### Requirement: Skip history list screen

The app SHALL provide a screen that lists skip records newest first, showing time, player name, title, artist and match reason.

#### Scenario: List shows newest first

- **WHEN** records exist for 10:00 and 10:05
- **THEN** the 10:05 record appears above the 10:00 record

#### Scenario: Empty state

- **WHEN** no records exist
- **THEN** the screen shows an empty-state message

### Requirement: Filter history by player

The history screen SHALL let the user select which players to show, using the same player list as the monitored players setting. Selecting a subset SHALL show only records from those players.

#### Scenario: Filter to one player

- **WHEN** records exist for YouTube Music and Spotify and the user selects only Spotify
- **THEN** only Spotify records are listed

### Requirement: Clear history

The history screen SHALL provide a clear action that asks for confirmation and then deletes all records.

#### Scenario: Clear confirmed

- **WHEN** the user confirms the clear action
- **THEN** all records are deleted and the empty state is shown

#### Scenario: Clear cancelled

- **WHEN** the user cancels the clear action
- **THEN** all records remain
