## Context

目前 `SkipCoordinator.onMetadata` 在規則命中、通過防抖動與連跳保護後，只做一件事：對該工作階段呼叫 `skipToNext()`（失敗時退回 `KEYCODE_MEDIA_NEXT`），再回報 `SkipEvent` 寫入 Room。規則存在 DataStore，關鍵字與正規表達式各是一個「每行一筆」的字串，簡體中文是一個布林值；`TextFilterEngine.evaluate` 回傳的 `MatchResult.reason` 是 `simplified-title`、`simplified-artist`、`keyword:<文字>` 或 `regex:<運算式>`。

在模擬器上用除錯探測（`QueueProbe`）實測 YouTube Music：`PlaybackState.customActions` 公開了 `thumbs_up_action`（Like）、`thumbs_down_action`（Dislike）、`shuffle_action`、`loop_mode_action`；`MediaController.ratingType` 為 2（上下拇指），`ACTION_SET_RATING` 在動作位元中，`metadata` 的 `USER_RATING` 目前為 `unrated`。YouTube 影片版的動作位元也含 `ACTION_SET_RATING`，但自訂動作尚未探測；Spotify 沒有帳號，尚未探測。

## Goals / Non-Goals

**Goals:**

- 每條規則可選「只跳過」（預設）或「不喜歡後跳過」，且對既有使用者與既有設定零影響。
- 只用播放器自己公開的官方動作，找不到就退回只跳過。
- 不因重複送出而「取消」使用者原有的不喜歡。
- 讓使用者看得到哪些紀錄已送出不喜歡。

**Non-Goals:**

- 「我不感興趣」、按讚、批次補標既有曲目、全域總開關。
- 任何 AccessibilityService、逆向或模擬點擊。

## Decisions

### 每條規則的旗標以「規則鍵」存放在獨立的 DataStore 項目

規則鍵格式與現有 `MatchResult.reason` 對齊：`simplified`（title 與 artist 共用）、`keyword:<文字>`、`regex:<運算式>`。新增 DataStore 項目 `dislike_rules`，編碼方式與 `keywords` 相同（每行一個鍵）。`AppSettings` 新增 `dislikeRules: Set<String>`，預設為空集合。

理由：既有的 `keywords`、`regexes` 保持 `List<String>` 不動，所以舊資料不需轉換，未寫入該項目就等於全部「只跳過」。拒絕的替代方案：把旗標編進每筆規則字串（需要改寫並遷移舊資料，也容易污染比對文字）；把規則改成物件清單（改動面太大）。

旗標生命週期：編輯規則文字時旗標跟著移到新文字；刪除規則時移除對應鍵；新增一條與舊刪除規則同文字的規則時不繼承。讀取時若鍵找不到對應規則就忽略，避免殘留資料影響行為。

### 命中後先送 dislike，再跳過

順序固定為「送出 dislike → 確認播放器是否自己換歌 → 必要時 `skipToNext()`」。播放器的媒體工作階段指令會依序處理，所以 dislike 套用在目前這首；若先跳過，目前曲目就換成下一首，dislike 會落在錯誤的曲目上。送出 dislike 失敗（例外）時只記錄 log，仍然照常跳過，紀錄的 `disliked` 為 false。

### 等播放器穩定再送，並且只在播放器沒有自己換歌時才跳過（實測後修正）

在真實 YouTube Music 上實測發現兩件事，因此修正原本「命中後立刻送 dislike 再跳過」的設計：

1. 曲目剛開始時送出的 dislike 不可靠（0.7 秒時未穩定生效），而且 `USER_RATING` 在換歌後一小段時間內可能是過期的，重複送出可能把已存在的不喜歡取消。因此標記規則的命中改成等 `DISLIKE_SETTLE_MS`（目前 2000 毫秒）才動作。
2. YouTube Music 在不喜歡生效後會在約 0.2 秒內自己換到下一首，再補一次 `skipToNext()` 會多跳一首。因此送出 dislike 後等 `DISLIKE_ADVANCE_CHECK_MS`（目前 600 毫秒），播放器沒有換歌才跳過。

代價：標記規則的跳過比單純跳過慢（實測約 3.5 秒是在 1.5 秒檢查窗口下量到的，現在預期約 2.7 秒，尚未重測），不受 0.5 秒目標約束。單純跳過的規則完全不變。這兩個時間只在一次模擬器實測中調整過。

### 辨識 dislike 動作的方式

在目前工作階段的 `PlaybackState.customActions` 中，找第一個 `action` 字串（不分大小寫）含 `thumbs_down` 或 `dislike` 的項目；若沒有，再找 `name` 不分大小寫等於 `Dislike` 的項目。找不到就視為這個播放器不支援，只跳過。辨識邏輯放成可單元測試的純函式，不依賴 Android 物件。

### 以使用者評分避免「取消」

很多播放器對已選取的按鈕再按一次會取消。因此送出前讀取 `MediaMetadata` 的 `USER_RATING`，分成四種狀態：評分未公開（UNKNOWN）、未評分（UNRATED）、已按讚（LIKED）、已不喜歡（DISLIKED）。只有 UNRATED 與 LIKED 才送出；DISLIKED 不送（已經達成目的，紀錄顯示未送出）；UNKNOWN 也不送，因為無法判斷會不會取消，寧可少做。這是保守設計，若實測發現某播放器永遠是 UNKNOWN 但送出沒有副作用，再以另一個變更放寬。

### 每首最多一次，且只在真的會跳過時送出

`SkipCoordinator` 已經用 `TrackIdentity` 做防抖動，同一首只評估一次。dislike 放在連跳保護檢查之後、跳過之前，所以總開關關閉、播放器未被監聽、重複 metadata、連跳保護暫停這幾種情況都不會送出。

### 紀錄資料庫升版

`SkipRecord` 新增 `disliked: Boolean`，Room 版本 1 → 2，migration 為 `ALTER TABLE skip_record ADD COLUMN disliked INTEGER NOT NULL DEFAULT 0`，舊紀錄保留並視為未送出。歷史頁在已送出的紀錄上顯示一個標記文字。

### 介面

每條關鍵字與正規表達式那一行下方加一個勾選框，標籤為「同時標為不喜歡」；簡體中文開關下方放同樣的勾選框，開關關閉時勾選框停用。規則頁在第一個勾選框上方顯示一行說明：「會同步到你的帳號並影響推薦，本 App 無法替你取消。」五種語言都補齊字串。

## Implementation Contract

**Behavior**

- 規則的「不喜歡」選項預設關閉。開啟後，該規則命中的曲目在被跳過前，會對同一個工作階段送出播放器公開的 dislike 動作。
- 其他情況（選項關閉、播放器沒公開動作、已是不喜歡、評分狀態不明、送出時丟出例外）一律只跳過，不顯示錯誤。
- 歷史頁每筆紀錄顯示是否已送出不喜歡。

**Interface / data shape**

- `SkipTarget` 新增三個成員：`dislikeActionId(): String?`（沒有公開時為 null）、`ratingState(): RatingState`（UNKNOWN／UNRATED／LIKED／DISLIKED）、`sendCustomAction(id: String)`。生產實作 `ControllerTarget` 以 `controller.playbackState.customActions`、`controller.metadata.getRating("android.media.metadata.USER_RATING")` 與 `controller.transportControls.sendCustomAction(id, null)` 實作。
- `SkipEvent` 新增 `disliked: Boolean`；`SkipRecord` 新增 `disliked: Boolean`。
- `AppSettings.dislikeRules: Set<String>`；DataStore 項目名稱 `dislike_rules`；規則鍵為 `simplified`、`keyword:<文字>`、`regex:<運算式>`。`SettingsStore` 新增設定單一規則旗標的方法，以及規則編輯時搬移或移除旗標的方法。

**Failure modes**

- `sendCustomAction` 例外：記 log，繼續跳過，`disliked = false`。
- 找不到 dislike 動作或評分不是 UNRATED／LIKED：靜默只跳過。
- Room migration 失敗不可接受：由單元測試用舊版資料庫檔案驗證舊紀錄保留。

**Acceptance criteria**

- 單元測試：`SkipCoordinator` 在各分支（旗標開／關、無動作、已不喜歡、評分未知、例外、連跳暫停、總開關關閉）送出 dislike 的次數與順序（dislike 先於 skip）。
- 單元測試：dislike 動作辨識函式（`thumbs_down_action`、含 `dislike` 的字串、只有 `name` 為 Dislike、完全沒有）。
- 單元測試：設定向下相容（未寫入 `dislike_rules` 時全部只跳過）、旗標隨編輯移動、刪除後消失。
- 單元測試：Room 1→2 migration 保留舊紀錄並將 `disliked` 設為 false。
- UI 測試：勾選框的顯示、切換、簡體開關關閉時停用、歷史頁標記。
- 手動驗證（需要使用者同意，因為會改動真實帳號的評分）：YouTube Music 與 YouTube 在真機或模擬器上各驗證一次送出不喜歡並確認介面上的拇指狀態改變；用 fake-player 的 Dislike 動作驗證 Spotify 路徑；結果寫進 `docs/queue-probe-notes.md`。

**Scope boundaries**

- 範圍內：上列的設定、協調器、紀錄、介面、五種語言、fake-player 的 Dislike 動作、文件與規格更新。
- 範圍外：我不感興趣、按讚、批次處理、全域總開關、任何非官方 API 手段、`filter-engine` 的比對邏輯。

## Risks / Trade-offs

- **帳號副作用**：不喜歡會寫進使用者的 Google 帳號，影響推薦，且本 App 無法復原。緩解：預設關閉、介面明確說明、只針對使用者主動勾選的規則。
- **誤傷**：關鍵字或簡體規則誤判時，會對不該不喜歡的曲目送出評分。緩解：不喜歡與跳過各自獨立選擇，使用者可以先只跳過，確認規則穩定後才開啟。
- **動作名稱非標準**：`thumbs_down_action` 是 YouTube Music 目前的自訂字串，播放器更新後可能改名，辨識失敗時退回只跳過。緩解：用字串包含判斷而非完全比對，並在文件記錄實測版本。
- **評分資訊可能滯後**：換曲時 `USER_RATING` 可能還是上一首的狀態。緩解：手動驗證時刻意測「前一首已不喜歡、下一首未評分」的情況；若有問題，改成等評分資訊與曲目一致後再送出，並重新評估延遲。
- **增加延遲**：多一次跨行程呼叫，預期為個位數毫秒，但這是估計值，驗證時以 log 的 `latencyMs` 量測確認。
