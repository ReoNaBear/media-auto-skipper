## ADDED Requirements

### Requirement: Record whether a dislike was sent

Each skip record SHALL include whether a dislike was sent for that skip. The history list SHALL show a marker on records where a dislike was sent. Records created before this field existed SHALL be kept and SHALL be treated as no dislike sent.

#### Scenario: Dislike sent

- **WHEN** a skip happens together with a dislike
- **THEN** the record is stored with dislike sent and the list shows the marker on it

#### Scenario: Skip only

- **WHEN** a skip happens without a dislike
- **THEN** the record is stored with no dislike and the list shows no marker

#### Scenario: Upgrade keeps old records

- **WHEN** the app is updated and the history already holds records
- **THEN** all records remain and none shows the marker
