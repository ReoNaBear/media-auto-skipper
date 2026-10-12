## ADDED Requirements

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

## MODIFIED Requirements

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

### Requirement: Official APIs only

The service SHALL send the dislike only through the media controller's custom action call using the action id that the player itself published, or through the media controller's rating call with a thumbs-down rating on a player that advertises it. It SHALL NOT use an accessibility service, simulate screen taps, or call undocumented interfaces of any player.

#### Scenario: Action id comes from the player

- **WHEN** a dislike is sent through a custom action
- **THEN** the action id passed to the call is the id read from the player's published custom actions

#### Scenario: Rating only when advertised

- **WHEN** a dislike is sent through the rating call
- **THEN** the player had advertised setting a thumbs up/down rating
