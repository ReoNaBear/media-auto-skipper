# filter-settings-ui Specification

## Purpose

Give the user a Jetpack Compose interface to grant the required system permission and to configure which players and rules are used for skipping.

## Requirements

### Requirement: Notification Access guidance

The app SHALL show the current Notification Access status on the main screen. When access is not granted, it SHALL show an explanation of why it is needed and a button that opens the system Notification Access settings screen.

#### Scenario: Permission missing

- **WHEN** the user opens the app without Notification Access granted
- **THEN** the main screen shows the explanation and a button that opens the system settings page

#### Scenario: Permission granted

- **WHEN** the user returns to the app after granting access
- **THEN** the main screen shows the service as active

---
### Requirement: Enable switch control

The main screen SHALL show the master enable switch with its current state. The switch SHALL be disabled with an explanation while Notification Access is not granted. When the user turns it on, the app SHALL remind the user that skipping continues after leaving the app until it is turned off or the device reboots.

#### Scenario: Turn on

- **WHEN** Notification Access is granted and the user turns the switch on
- **THEN** the switch shows on and the service starts acting on monitored sessions

#### Scenario: Switch unavailable without permission

- **WHEN** Notification Access is not granted
- **THEN** the switch cannot be turned on and the screen explains why

---
### Requirement: Blacklist keyword editing

The app SHALL show the keyword list and SHALL let the user add, edit and delete keywords and regular expression rules. A blank entry or an invalid regular expression SHALL be rejected with a visible error.

#### Scenario: Add keyword

- **WHEN** the user enters "Nightcore" and confirms
- **THEN** the keyword appears in the list and is applied to later tracks

#### Scenario: Blank rejected

- **WHEN** the user confirms an entry containing only spaces
- **THEN** the entry is rejected and the list is unchanged

---
### Requirement: Rule and player toggles

The app SHALL provide a toggle for the Simplified Chinese rule and a list of monitored players. The monitored list SHALL default to YouTube Music, YouTube and Spotify, and the user SHALL be able to add another player from the installed media apps.

#### Scenario: Disable simplified rule

- **WHEN** the user turns off the Simplified Chinese rule
- **THEN** tracks that only match that rule are no longer skipped

#### Scenario: Default players

- **WHEN** the app runs for the first time
- **THEN** YouTube Music, YouTube and Spotify are selected as monitored players

---
### Requirement: Settings persistence

The app SHALL store keywords, regular expression rules, toggles and the monitored player list in DataStore and SHALL restore them after the app or device restarts.

#### Scenario: Settings survive restart

- **WHEN** the user adds keyword "DJ" and restarts the device
- **THEN** the keyword is still in the list and applied by the service
