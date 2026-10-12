# media-monitor-service Specification

## Purpose

Observe the system's active media sessions in the background and skip tracks that match the filter, without playing any media itself. It works when the screen is off and when the phone is connected to a car, because the service runs on the phone.

## Requirements

### Requirement: Observe active media sessions

The service SHALL be a NotificationListenerService and SHALL use MediaSessionManager.getActiveSessions with its own component name to obtain active MediaControllers. It SHALL read the package name, title and artist from each monitored controller's metadata, and SHALL update when sessions or metadata change.

#### Scenario: Metadata change is observed

- **WHEN** a monitored player starts a new track
- **THEN** the service receives the package name, title and artist of that track

#### Scenario: Unmonitored player ignored

- **WHEN** a player whose package is not in the monitored list changes track
- **THEN** the service takes no action

---
### Requirement: Record each skip

Each time the service issues a skip, it SHALL write one record to the skip history containing the time, package name, title, artist and match reason, including skips that used the key event fallback.

#### Scenario: Skip is recorded

- **WHEN** the service skips a track titled "华语精选" in a monitored player
- **THEN** one history record with that title, the package name and the match reason is stored

---
### Requirement: Skip matching tracks

When the current track of a monitored session matches the filter, the service SHALL call that controller's transportControls.skipToNext(). The service SHALL log the time from receiving the metadata change to issuing the skip, so the 0.5 second target can be measured. If skipToNext() throws, the service SHALL send a KEYCODE_MEDIA_NEXT key event pair targeted at that session instead.

#### Scenario: Matching track is skipped

- **WHEN** the current track title is "华语精选" in a monitored player and the filter matches
- **THEN** the service calls skipToNext() on that player's controller

#### Scenario: Non-matching track is untouched

- **WHEN** the current track matches no rule
- **THEN** the service sends no media command

#### Scenario: Fallback key event

- **WHEN** skipToNext() throws an exception for a matching track
- **THEN** the service sends KEYCODE_MEDIA_NEXT down and up events to that session

---
### Requirement: Debounce repeated metadata

The service SHALL identify a track by package name, title and artist, and SHALL act at most once for each distinct identity per consecutive occurrence. Repeated metadata callbacks for an identity that was already skipped SHALL be ignored until a different identity is observed.

#### Scenario: Duplicate callbacks cause one skip

- **WHEN** the player emits the same matching metadata three times within one second
- **THEN** skipToNext() is called exactly once

---
### Requirement: Skip loop protection

The service SHALL stop skipping after 20 consecutive skips within 60 seconds in the same session, and SHALL resume only after a track that does not match is observed or 60 seconds pass without a skip. When protection triggers, the service SHALL show a notification to the user.

#### Scenario: Whole playlist matches

- **WHEN** every track in a playlist matches and 20 skips occur within 60 seconds
- **THEN** the service stops skipping and posts a notification that skipping is paused

---
### Requirement: Background and car operation

The service SHALL keep working while the screen is off and while the phone is connected through Bluetooth or Android Auto, and SHALL NOT play, record or capture any audio.

#### Scenario: Screen off

- **WHEN** the screen is off and a matching track starts
- **THEN** the service skips it (the master switch being on)

#### Scenario: Car connected

- **WHEN** the phone is connected to Android Auto and a matching track starts in the monitored player
- **THEN** the track is skipped and the car display shows the next track

---
### Requirement: Master enable switch

The service SHALL act on media sessions only while the master enable switch is on. The switch SHALL default to off. While it is off, the service SHALL ignore all sessions and send no media command. When the switch is on, it SHALL stay on while the app is in the background or the screen is off. After the device reboots, the switch SHALL be off until the user turns it on again in the app.

#### Scenario: Off by default

- **WHEN** the app is installed and Notification Access is granted but the user has not turned the switch on
- **THEN** a matching track is not skipped

#### Scenario: Stays on after leaving the app

- **WHEN** the user turns the switch on and then closes the app and turns the screen off
- **THEN** a matching track is still skipped

#### Scenario: Reset after reboot

- **WHEN** the switch is on and the device reboots
- **THEN** the switch is off after boot and a matching track is not skipped

---
### Requirement: Behavior without permission

If Notification Access is not granted, the service SHALL NOT run and the app SHALL display the permission guide instead of failing silently.

#### Scenario: Permission revoked

- **WHEN** the user revokes Notification Access
- **THEN** no skipping occurs and the main screen shows that permission is required
