# Queue probe notes (task 4.6)

Purpose: decide whether pre-skipping the upcoming track is feasible. Method: `adb shell am broadcast -a com.mediaautoskipper.PROBE -p com.mediaautoskipper` on a debug build, which logs `MediaController.metadata`, `playbackState` and `queue` (tag `MasProbe`).

Environment: Android emulator, API 37 (Google APIs + Play Store image), 2026-10-12. Player app versions were not recorded.

## Results

| Player | Metadata | Queue | skipToNext advertised | skipToQueueItem advertised |
| --- | --- | --- | --- | --- |
| YouTube Music (real app, logged in) | title and artist | **Readable: 25 items with title and artist (subtitle) while playing a song search result** | yes | no |
| YouTube (real app, logged in) | title; artist empty at first, filled in a moment later | `null` (not published) | no | no |
| Spotify | not tested (no account); the fake player stands in | not tested | not tested | not tested |

Observations:

- YouTube Music: the queue window moves with playback (after one skip the list started at the new current item and was 24 items long). The first metadata callback for a track can arrive with an empty title and artist, then title only, then the full pair; the coordinator evaluates each distinct combination.
- YouTube: a single video has no queue, so there is no "next track" to read. `skipToNext` was not advertised, so skipping a single video through the session is not expected to work. Not yet confirmed by a successful test (the attempt was cut short by slow search on the emulator).
- A real skip worked on YouTube Music: with the keyword "Renegade" set, the track "Renegade (feat. Taylor Swift)" was skipped through `skipToNext()` and playback moved to the next queue item. Service-side time from metadata arrival to issuing the skip was 4 ms. This excludes the player's own delay before it publishes metadata, so it does not prove the 0.5 s target.

## Conclusion so far

- Pre-skip looks **feasible for YouTube Music only** (queue is readable with titles). `skipToQueueItem` is not advertised, so jumping straight to a later item may not be possible; the practical option is calling `skipToNext()` repeatedly when the next item matches. Not yet tried.
- Pre-skip is **not possible for YouTube** through the media session (no queue).
- Spotify is unknown.

## Reboot check (task 4.5, partial)

On the emulator (2026-10-12): master switch turned on (UI showed checked), then `adb reboot`. After boot `Settings.Global.BOOT_COUNT` went 3 → 4 and the switch was off again, without any user action. This confirms the reset-after-reboot rule on the emulator; it was not repeated on a physical phone.

## Screen-off check (task 4.5, partial)

On the emulator with real YouTube Music, keyword "wish you were", screen state `Asleep` throughout (`dumpsys power`): `cmd media_session dispatch next` moved playback to "Wish You Were Here"; the service skipped it (`skipped ... reason=keyword:wish you were fallback=false latencyMs=3`) and the next track, "Wdym", was publishing metadata about 0.1 s later. Note: a plain `KEYCODE_MEDIA_NEXT` via `adb shell input` did not reach the player in this setup, so `media_session dispatch` was used. Not repeated on a physical phone.

## Still to do

- Spotify (needs an account) and a YouTube skip test inside a playlist.
- Android Auto (Desktop Head Unit) checks, and screen-off with real YouTube / Spotify (task 4.5).
