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

## Dislike probe and test (change `dislike-on-skip`)

Emulator (API 37), real apps, 2026-10-12, probe extended to print rating and custom actions.

| Player | Custom actions | Rating | Result |
| --- | --- | --- | --- |
| YouTube Music | `thumbs_up_action` (Like), `shuffle_action`, `loop_mode_action`, `thumbs_down_action` (Dislike) | `ratingType=2` (thumbs), `ACTION_SET_RATING` advertised, `USER_RATING` published | Dislike works through `sendCustomAction("thumbs_down_action")` |
| YouTube (video app) | none | `ratingType=2` and `ACTION_SET_RATING` advertised, but `USER_RATING` is null | No dislike action to send, so a marked rule only skips. `transportControls.setRating(...)` might work; not tried |
| Spotify | not tested (no account) | not tested | The fake player publishes the same action and rating; the full path works there |

What was observed on YouTube Music:

- A dislike sent on a track that had been playing for a while was applied and YouTube Music moved to the next track by itself about 0.2 s later. A skip on top of that would drop a second track, so the app now sends the skip only when the player did not move on within 0.6 s.
- A dislike sent 0.7 s after the track started was not reliably applied; the rating in the metadata can also be stale for a moment after a track change, which is how a repeated send can toggle an existing dislike off. Waiting 2 s before sending worked in the final run (dislike sent at +1.8 s, track changed 0.15 s later). This was tuned on one session, so treat 2 s as a starting point.
- The metadata rating (`USER_RATING`) lagged behind the real state, so it is a guard, not a guarantee.
- Latency with a dislike: 3.5 s from the first metadata to the finished skip was measured with a 1.5 s advance check; with the final 0.6 s check, about 2.7 s is expected (not measured again). Skip-only rules keep the earlier 1 to 20 ms service-side time.
- Effect on the account: tracks used in these tests were disliked on the signed-in YouTube Music account, among them: TikTok (Blueface), SMOOTH DJ LOVE TIKTOK, Call of Silence (TikTok remix), plus two from timing experiments, "Choose a Player" and "Always Loving Miracles".

