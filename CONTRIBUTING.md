# Contributing / Developer guide

English first, 繁體中文 follows below. / 英文在前,繁體中文在後。

## Developer guide

Media Auto-Skipper is version 0.1.0. Contributions are welcome: bug reports, translations, and code. By contributing you agree your work is released under the [MIT license](LICENSE).

### Ground rules

These are the project's fixed constraints; a change that breaks one will not be accepted.

- Official Android APIs only. No AccessibilityService, no reverse engineering of other apps, no audio capture.
- The app never plays audio and has no network access.
- Android only (iOS has no equivalent of reading other apps' media sessions).
- Matching a keyword in one language must never fire on another language (no Simplified/Traditional conversion).

### Set up

Requirements: JDK 17+, Android SDK (platform 35). Create `local.properties` with `sdk.dir=<path>` (forward slashes on Windows), then:

```
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest :filter-engine:test
./gradlew lintDebug
```

Stack: Kotlin 2.0, Jetpack Compose (Material 3), Room, DataStore, Gradle 8.10 / AGP 8.7, Robolectric for unit and UI tests. minSdk 26, targetSdk 35.

### Project layout

```
filter-engine/   pure Kotlin matching (no Android dependency)
  TextFilterEngine.kt     Simplified/keyword/regex decisions
  SimplifiedChars.kt      table of Simplified-only characters
app/             the Android app (com.mediaautoskipper)
  monitor/MediaMonitorService.kt   NotificationListenerService, reads media sessions
  monitor/SkipCoordinator.kt       debounce, loop protection, skip + key fallback
  monitor/QueueProbe.kt            debug-only queue/metadata dump
  settings/                        DataStore settings, rule edit logic, master switch
  history/                         Room database of skips
  ui/                              Compose screens (Home, Rules, History, language)
  src/main/res/values*/strings.xml one file per language
fake-player/     dev-only test player (uses Spotify's package name)
docs/            queue-probe-notes.md, screenshots, Spectra specs
```

### How a skip happens

1. `MediaMonitorService` finds active sessions through `MediaSessionManager` and registers a `MediaController` callback for each monitored package.
2. On a metadata change it passes package, title and artist to `SkipCoordinator`.
3. `SkipCoordinator` checks the master switch, the monitored list, duplicates (same track) and the loop limit (20 skips in 60 s), then asks `TextFilterEngine` for a decision.
4. On a match it calls `transportControls.skipToNext()`; if that throws it sends `KEYCODE_MEDIA_NEXT` to the same session, and records the skip in Room.
5. If the matched rule is marked "also dislike" (`DislikeRules`, stored in DataStore as `dislike_rules`), the coordinator waits `DISLIKE_SETTLE_MS`, sends the player's dislike custom action (found by `DislikeSupport.findActionId`, only when `RatingState` allows it), then skips only if the player did not move on by itself within `DISLIKE_ADVANCE_CHECK_MS`.

### Testing

- Unit and UI tests run on the JVM with Robolectric; there is no emulator needed for `testDebugUnitTest`.
- Manual testing without accounts: install `fake-player` (`./gradlew :fake-player:assembleDebug`). It publishes a media session with a short playlist; `adb shell am start -n com.spotify.music/fakeplayer.PlayerActivity --ez advance true` advances it. Uninstall it before installing the real Spotify.
- A debug build answers `adb shell am broadcast -a com.mediaautoskipper.PROBE -p com.mediaautoskipper`, logging queue, metadata and playback state under the tag `MasProbe`. Service decisions are logged under `MediaAutoSkipper`.
- Windows + Git Bash: set `MSYS_NO_PATHCONV=1` before `adb` commands that use `/sdcard` paths.

### Translations

UI strings live in `app/src/main/res/values*/strings.xml` (en, ko, ja, zh-CN, zh-TW). Add a language by creating a `values-<code>` folder, adding it to `res/xml/locales_config.xml` and to `LanguageChoice`. Native-speaker review of Korean and Japanese is especially welcome. `./gradlew lintDebug` fails on a missing translation.

### Spectra workflow

Requirements live in `docs/spectra/specs/` and finished changes in `docs/spectra/changes/archive/`. For a behaviour change, update the relevant spec first, then the code and tests. Using the Spectra CLI is optional for outside contributors.

### Pull requests

- One topic per PR, with tests for logic changes (matching, skip rules, settings).
- Run `./gradlew testDebugUnitTest :filter-engine:test lintDebug` before opening it.
- Say how you verified UI or player behaviour (emulator, device, which player and version).

### Good first contributions

- Verify on a physical phone and in a real car (Android Auto), and report results.
- Test real Spotify and skipping inside a YouTube playlist.
- Review the Simplified-character table against a dictionary; review ko/ja translations.
- Pre-skip the next track on YouTube Music (its queue is readable; see `docs/queue-probe-notes.md`).

---

## 開發者指南

Media Auto-Skipper 目前是 0.1.0 版。歡迎回報問題、貢獻翻譯與程式碼。提交貢獻即表示你同意以 [MIT 授權](LICENSE)釋出你的內容。

### 基本原則

以下是專案固定的限制,違反的修改不會被接受。

- 只使用 Android 官方 API。不用 AccessibilityService、不逆向其他 App、不擷取音訊。
- App 不播放聲音,也沒有網路權限。
- 只做 Android(iOS 沒有讀取其他 App 媒體工作階段的機制)。
- 某語言的關鍵字不可誤中另一種語言(不做繁簡轉換)。

### 環境設定

需求:JDK 17 以上、Android SDK(platform 35)。建立 `local.properties`,內容為 `sdk.dir=<路徑>`(Windows 用正斜線),然後:

```
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest :filter-engine:test
./gradlew lintDebug
```

技術:Kotlin 2.0、Jetpack Compose(Material 3)、Room、DataStore、Gradle 8.10 / AGP 8.7,單元與介面測試用 Robolectric。minSdk 26、targetSdk 35。

### 專案結構

```
filter-engine/   純 Kotlin 的比對邏輯(不依賴 Android)
  TextFilterEngine.kt     簡體/關鍵字/正規表達式的判斷
  SimplifiedChars.kt      簡體專有字表
app/             Android App(com.mediaautoskipper)
  monitor/MediaMonitorService.kt   NotificationListenerService,讀取媒體工作階段
  monitor/SkipCoordinator.kt       防抖動、連跳保護、跳歌與按鍵備援
  monitor/QueueProbe.kt            僅除錯版的佇列/metadata 傾印
  settings/                        DataStore 設定、規則編輯邏輯、總開關
  history/                         Room 跳過紀錄資料庫
  ui/                              Compose 畫面(首頁、規則、紀錄、語言)
  src/main/res/values*/strings.xml 每種語言一個檔
fake-player/     僅供開發的測試播放器(借用 Spotify 套件名稱)
docs/            queue-probe-notes.md、截圖、Spectra 規格
```

### 一次跳歌如何發生

1. `MediaMonitorService` 透過 `MediaSessionManager` 找到使用中的工作階段,替每個受監聽的套件註冊 `MediaController` 回呼。
2. metadata 變動時,把套件、標題、藝人交給 `SkipCoordinator`。
3. `SkipCoordinator` 依序檢查總開關、監聽清單、重複(同一首)與連跳上限(60 秒 20 次),再請 `TextFilterEngine` 判斷。
4. 命中時呼叫 `transportControls.skipToNext()`;若丟出例外,改對同一工作階段送 `KEYCODE_MEDIA_NEXT`,並把這次跳過寫入 Room。
5. 若命中的規則勾選了「同時標為不喜歡」(`DislikeRules`,以 `dislike_rules` 存在 DataStore),協調器會先等 `DISLIKE_SETTLE_MS`,再送出播放器的不喜歡自訂動作(用 `DislikeSupport.findActionId` 找、且 `RatingState` 允許時才送),之後只有在播放器沒有在 `DISLIKE_ADVANCE_CHECK_MS` 內自己換歌時才跳過。

### 測試

- 單元與介面測試用 Robolectric 在 JVM 跑,`testDebugUnitTest` 不需要模擬器。
- 沒有帳號時的手動測試:安裝 `fake-player`(`./gradlew :fake-player:assembleDebug`),它會發布一個含簡短播放清單的媒體工作階段;`adb shell am start -n com.spotify.music/fakeplayer.PlayerActivity --ez advance true` 可讓它前進。安裝真正的 Spotify 前請先解除安裝。
- 除錯版會回應 `adb shell am broadcast -a com.mediaautoskipper.PROBE -p com.mediaautoskipper`,把佇列、metadata、播放狀態印到標籤 `MasProbe`;服務的判斷結果印在標籤 `MediaAutoSkipper`。
- Windows + Git Bash:使用 `/sdcard` 路徑的 `adb` 指令前,先設定 `MSYS_NO_PATHCONV=1`。

### 翻譯

介面文字在 `app/src/main/res/values*/strings.xml`(en、ko、ja、zh-CN、zh-TW)。新增語言:建立 `values-<代碼>` 資料夾,加入 `res/xml/locales_config.xml` 與 `LanguageChoice`。特別歡迎母語者校對韓文與日文。缺少翻譯時 `./gradlew lintDebug` 會失敗。

### Spectra 流程

需求寫在 `docs/spectra/specs/`,完成的變更放在 `docs/spectra/changes/archive/`。要改變行為時,先更新對應規格,再改程式與測試。外部貢獻者不一定要使用 Spectra CLI。

### Pull request

- 一個 PR 一個主題;邏輯變更(比對、跳歌規則、設定)要附測試。
- 開 PR 前執行 `./gradlew testDebugUnitTest :filter-engine:test lintDebug`。
- 說明介面或播放器行為如何驗證(模擬器或實機、哪個播放器與版本)。

### 適合新手的貢獻

- 在實體手機與實車(Android Auto)上驗證並回報結果。
- 測試真實 Spotify,以及在 YouTube 播放清單內跳歌。
- 對照字典檢查簡體字表;校對韓文與日文翻譯。
- 在 YouTube Music 上提前跳過下一首(它的佇列可讀,見 `docs/queue-probe-notes.md`)。
