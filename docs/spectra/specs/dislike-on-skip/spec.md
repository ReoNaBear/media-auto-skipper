# dislike-on-skip Specification

## Purpose

Let matching tracks teach the player what the user does not want, by sending the player's own published "dislike" action before skipping, only for rules the user opted in. Uses official media session APIs only.

## Requirements

### Requirement: Send dislike before skipping for opted-in rules

When a track matches a rule that the user marked "dislike and skip", the service SHALL send the dislike action that the player published in its playback state custom actions to that player's session, and SHALL then skip to the next track unless the player already moved on (see the delay requirement below). The dislike SHALL be sent before any skip. A matching rule that is not marked SHALL only skip, as before.

#### Scenario: Marked keyword matches

- **WHEN** the keyword "Mix" is marked "dislike and skip", YouTube Music publishes a custom action with id `thumbs_down_action`, the current user rating is unrated, and the track "Summer Mix 2024" starts
- **THEN** the service sends the custom action `thumbs_down_action` to that session and then calls skip to next, in that order

#### Scenario: Unmarked rule matches

- **WHEN** a track matches a rule that is not marked
- **THEN** the service skips the track and sends no dislike

#### Scenario: Simplified Chinese rule is marked

- **WHEN** the Simplified Chinese rule is marked and a track with a Simplified-only title starts
- **THEN** the dislike is sent before the skip, the same as for a keyword

---
### Requirement: Wait for the player and avoid a double skip

For a marked rule on a player that is disliked through its custom action, the service SHALL wait a settle delay after the track's first metadata before sending the dislike, and SHALL skip to the next track only if the player has not already moved to a different track within a short check delay after the dislike was sent. A pending action SHALL be dropped when the track changes before it runs. Rules that are not marked, marked rules on a player that has neither a dislike action nor rating support, and marked rules on a custom-action player whose track rating is already a dislike or not published, SHALL NOT be delayed. When the delay ends, the service SHALL check again that the master switch is on, that the player is still monitored and that the rule still matches, using the current settings. Marked rules on a player that is disliked through the rating call follow the rating requirement instead and do not use the check delay.

#### Scenario: Settle delay

- **WHEN** a marked rule matches a newly started track on a custom-action player
- **THEN** nothing is sent until the settle delay has passed

#### Scenario: Player moves on by itself

- **WHEN** the player changes to another track after the dislike was sent and before the check delay ends
- **THEN** the service sends no skip, and the history record shows a skip with the dislike sent

#### Scenario: Player stays on the track

- **WHEN** the player still shows the same track when the check delay ends
- **THEN** the service skips to the next track

#### Scenario: Player without any dislike method

- **WHEN** a marked rule matches on a player that publishes no dislike action and does not advertise thumbs up/down rating
- **THEN** the service skips at once, without the settle delay

#### Scenario: Switch or rules change during the delay

- **WHEN** the master switch is turned off, the rule is removed, or the player is no longer monitored while a delay is running
- **THEN** the pending dislike and skip are dropped, and if only the rule's dislike option was turned off the track is still skipped without a dislike

#### Scenario: Rating already a dislike or not published

- **WHEN** a marked rule matches a custom-action player's track whose rating is already thumbs-down or is not published
- **THEN** the service skips at once, without the settle delay

#### Scenario: Track changes during the delay

- **WHEN** another track starts before the settle delay ends
- **THEN** the pending dislike and skip are dropped


<!-- @trace
source: rating-dislike-fallback
updated: 2026-10-12
code:
  - app/src/main/kotlin/com/mediaautoskipper/monitor/MediaMonitorService.kt
  - app/src/main/kotlin/com/mediaautoskipper/monitor/SkipCoordinator.kt
  - app/src/main/kotlin/com/mediaautoskipper/monitor/DislikeSupport.kt
tests:
  - app/src/test/kotlin/com/mediaautoskipper/monitor/DislikeSupportTest.kt
  - app/src/test/kotlin/com/mediaautoskipper/monitor/SkipCoordinatorTest.kt
-->

---
### Requirement: Recognize the player's dislike action

The service SHALL treat the first custom action whose action id contains `thumbs_down` or `dislike` (case-insensitive) as the dislike action. If none matches, it SHALL treat the first custom action whose display name equals "Dislike" (case-insensitive) as the dislike action. If there is no such action, the player does not support dislike.

#### Scenario: Action id matches

- **WHEN** the custom actions are `thumbs_up_action`, `shuffle_action`, `loop_mode_action` and `thumbs_down_action`
- **THEN** `thumbs_down_action` is the dislike action

#### Scenario: Only the name matches

- **WHEN** the only candidate is a custom action with id `action_7` and name "Dislike"
- **THEN** `action_7` is the dislike action

#### Scenario: No dislike action

- **WHEN** the player publishes no custom action matching either rule
- **THEN** the player is treated as not supporting dislike

---
### Requirement: Fall back to skip only without error

If the player has no dislike action, or sending it throws, the service SHALL still skip the track, SHALL NOT show an error to the user, and SHALL record that no dislike was sent.

#### Scenario: Player without dislike

- **WHEN** a marked rule matches on a player that publishes no dislike action
- **THEN** the track is skipped and the history record shows no dislike was sent

#### Scenario: Sending throws

- **WHEN** sending the custom action throws an exception
- **THEN** the track is still skipped and the history record shows no dislike was sent

---
### Requirement: Never undo an existing dislike

On the custom action path, before sending, the service SHALL read the track's user rating from the session metadata. It SHALL send the dislike only when the rating is published and is either unrated or thumbs-up. It SHALL NOT send when the rating is already thumbs-down or when the rating is not published. This requirement does not apply to the rating call, which sets a rating instead of toggling one.

#### Scenario: Already disliked

- **WHEN** a marked rule matches a custom-action player's track whose user rating is thumbs-down
- **THEN** no dislike is sent, the track is skipped, and the history record shows no dislike was sent

#### Scenario: Rating not published

- **WHEN** a marked rule matches on a custom-action player whose metadata has no user rating
- **THEN** no dislike is sent and the track is skipped

#### Scenario: Previously liked

- **WHEN** a marked rule matches a custom-action player's track whose user rating is thumbs-up
- **THEN** the dislike is sent and the track is skipped


<!-- @trace
source: rating-dislike-fallback
updated: 2026-10-12
code:
  - app/src/main/kotlin/com/mediaautoskipper/monitor/MediaMonitorService.kt
  - app/src/main/kotlin/com/mediaautoskipper/monitor/SkipCoordinator.kt
  - app/src/main/kotlin/com/mediaautoskipper/monitor/DislikeSupport.kt
tests:
  - app/src/test/kotlin/com/mediaautoskipper/monitor/DislikeSupportTest.kt
  - app/src/test/kotlin/com/mediaautoskipper/monitor/SkipCoordinatorTest.kt
-->

---
### Requirement: Dislike only when a skip would happen

The service SHALL send a dislike at most once per track and only at the point where it would also skip: not when the master switch is off, not for players that are not monitored, not for repeated metadata of the same track, and not while loop protection has paused skipping.

#### Scenario: Master switch off

- **WHEN** the master switch is off and a marked rule would match
- **THEN** no dislike is sent

#### Scenario: Repeated metadata

- **WHEN** the same track publishes metadata three times
- **THEN** at most one dislike is sent

#### Scenario: Loop protection paused

- **WHEN** loop protection has paused skipping for a player and a marked rule matches
- **THEN** no dislike is sent

---
### Requirement: Official APIs only

The service SHALL send the dislike only through the media controller's custom action call using the action id that the player itself published, or through the media controller's rating call with a thumbs-down rating on a player that advertises it. It SHALL NOT use an accessibility service, simulate screen taps, or call undocumented interfaces of any player.

#### Scenario: Action id comes from the player

- **WHEN** a dislike is sent through a custom action
- **THEN** the action id passed to the call is the id read from the player's published custom actions

#### Scenario: Rating only when advertised

- **WHEN** a dislike is sent through the rating call
- **THEN** the player had advertised setting a thumbs up/down rating


<!-- @trace
source: rating-dislike-fallback
updated: 2026-10-12
code:
  - app/src/main/kotlin/com/mediaautoskipper/monitor/MediaMonitorService.kt
  - app/src/main/kotlin/com/mediaautoskipper/monitor/SkipCoordinator.kt
  - app/src/main/kotlin/com/mediaautoskipper/monitor/DislikeSupport.kt
tests:
  - app/src/test/kotlin/com/mediaautoskipper/monitor/DislikeSupportTest.kt
  - app/src/test/kotlin/com/mediaautoskipper/monitor/SkipCoordinatorTest.kt
-->

---
### Requirement: Dislike through the rating API when there is no dislike action

When a marked rule matches on a player that publishes no dislike custom action but advertises setting a thumbs up/down rating, the service SHALL send a thumbs-down rating to that player's session through the media controller's rating call, and SHALL then skip to the next track. The dislike method SHALL be chosen in this order: the player's dislike custom action, otherwise the rating call, otherwise no dislike. A rating dislike SHALL NOT be blocked by an unpublished current rating, because setting the same rating again is harmless. The service SHALL NOT wait to see whether the player moved on by itself after a rating dislike, and SHALL skip right after sending it. The service SHALL wait a named settle delay after the track's first metadata before sending a rating dislike, because on the YouTube video app a rating sent at once was not applied and a skip sent while the video was loading was ignored; the delay measured to work is 1.5 seconds. If the same track shows up again after the rating was sent, the service SHALL NOT rate it again and SHALL try the skip again.

#### Scenario: Player with rating support but no custom action

- **WHEN** a marked rule matches the track "Dai Dai (Lyrics)" on a player that publishes no custom actions but advertises thumbs up/down rating
- **THEN** the service sends a thumbs-down rating to that session and then calls skip to next, in that order, and the history record shows a dislike was sent

#### Scenario: Settle delay before the rating

- **WHEN** a marked rule matches a newly started video on a rating-capable player
- **THEN** nothing is sent until the settle delay has passed, then the rating is sent and the video is skipped

#### Scenario: Same track again after the rating

- **WHEN** the same track shows up again after its rating was sent, for example because the first skip was ignored
- **THEN** no second rating is sent and the skip is tried again

#### Scenario: Custom action wins over rating

- **WHEN** a player publishes a dislike custom action and also advertises rating
- **THEN** the service uses the custom action path and sends no rating

#### Scenario: No custom action and no rating support

- **WHEN** a marked rule matches on a player that publishes no dislike action and does not advertise thumbs up/down rating
- **THEN** the service only skips, at once, and the history record shows no dislike was sent

#### Scenario: Rating not published

- **WHEN** a marked rule matches on a rating-capable player whose metadata has no user rating
- **THEN** the thumbs-down rating is still sent

#### Scenario: Sending the rating throws

- **WHEN** sending the rating throws an exception
- **THEN** the track is still skipped and the history record shows no dislike was sent

#### Scenario: Only thumbs up/down ratings count

- **WHEN** a player advertises setting a rating but its rating type is stars or hearts
- **THEN** it is treated as not supporting rating dislike

<!-- @trace
source: rating-dislike-fallback
updated: 2026-10-12
code:
  - app/src/main/kotlin/com/mediaautoskipper/monitor/MediaMonitorService.kt
  - app/src/main/kotlin/com/mediaautoskipper/monitor/SkipCoordinator.kt
  - app/src/main/kotlin/com/mediaautoskipper/monitor/DislikeSupport.kt
tests:
  - app/src/test/kotlin/com/mediaautoskipper/monitor/DislikeSupportTest.kt
  - app/src/test/kotlin/com/mediaautoskipper/monitor/SkipCoordinatorTest.kt
-->