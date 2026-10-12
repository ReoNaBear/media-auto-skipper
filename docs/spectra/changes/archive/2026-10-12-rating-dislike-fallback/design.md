## Context

0.2.x 的 `SkipCoordinator` 對勾選規則的命中：若 `SkipTarget.dislikeActionId()` 有值且 `ratingState().allowsDislike`，等 `DISLIKE_SETTLE_MS`（2000 毫秒）、再次檢查總開關與設定、送出自訂動作、再等 `DISLIKE_ADVANCE_CHECK_MS`（600 毫秒）看播放器有沒有自己換歌，沒有才 `skipToNext()`。沒有自訂動作時立刻跳過。

實測（見 `docs/queue-probe-notes.md` 最後一節）：YouTube 影片版 `customActions` 為 0，但 `ACTION_SET_RATING` 在動作位元中且 `ratingType=2`；`setRating(Rating.newThumbRating(false))` 讓倒讚按鈕變實心，`setRating(Rating.newUnratedRating(RATING_THUMB_UP_DOWN))` 變回空心；`userRating` 恆為 null；YouTube 不會因評分而自己換影片。YouTube Music 在曲目剛開始時送出的不喜歡不可靠，影片版是否有同樣情形尚未實測。

## Goals / Non-Goals

**Goals:**

- 讓沒有 dislike 自訂動作、但支援上下拇指評分的播放器也能在勾選規則命中時送出不喜歡。
- 不改變 YouTube Music（自訂動作路徑）的行為與時間常數。
- 評分路徑盡量不拖慢跳過。

**Non-Goals:**

- 我不感興趣、按讚、讀取目前評分、任何非官方手段。

## Decisions

### 選擇不喜歡方式的順序

新增純函式 `DislikeSupport.chooseMethod(actionId, supportsRating)`，回傳 `CUSTOM_ACTION`、`RATING` 或 `NONE`：有自訂動作 id 就是 `CUSTOM_ACTION`；否則 `supportsRating` 為真就是 `RATING`；否則 `NONE`。理由：自訂動作路徑已在真機驗證，優先使用以保證 YouTube Music 零變動；評分只當備援。

### 「支援評分」的判斷

`ControllerTarget.supportsRating()` 為真，當且僅當 `PlaybackState.actions` 含 `ACTION_SET_RATING` 且 `MediaController.ratingType` 為 `Rating.RATING_THUMB_UP_DOWN`。只支援星等或愛心的播放器不算，因為倒讚沒有明確對應。

### 評分路徑的行為

送出 `setRating(Rating.newThumbRating(false))`，因為這是設定型，重送無害，所以：不套用「評分未公開就不送」「已是不喜歡就不送」的限制；不做換歌檢查（YouTube 不會自己換影片），送出後立刻 `skipToNext()`。每首曲目仍只送一次（沿用 `dislikedTitle`），避免部分 metadata 造成重複。

### 評分路徑的等待時間由實測決定

在 `SkipCoordinator` 新增常數 `RATING_SETTLE_MS`。先假設 0，再於真實 YouTube 影片版實測。實測結果（模擬器、一次）：立刻送出時評分不生效、跳過也被忽略；等 0.8 秒時第一次跳過仍被忽略；等 1.5 秒時評分生效且跳過成功，所以 `RATING_SETTLE_MS = 1500`。這個數字只在一次模擬器測試中量過，較快的手機可能可以更短。若同一支影片在評分送出後再次出現（例如第一次跳過被忽略），不重複評分，但重試跳過。

### 等待期間重新檢查

若 `RATING_SETTLE_MS` 大於 0，沿用 0.2.2 的做法：等待結束時重新檢查總開關、監聽清單與規則並使用當下的設定。若為 0，則在同一個呼叫中完成，不需要排程。

## Implementation Contract

**Behavior**

- 勾選規則命中、播放器沒有 dislike 自訂動作、但宣告支援上下拇指評分：先送倒讚評分，再立刻（或等 `RATING_SETTLE_MS` 後）跳過；紀錄顯示已送出不喜歡。
- 有自訂動作的播放器：行為與 0.2.2 完全相同。
- 兩者都沒有：立刻只跳過。

**Interface / data shape**

- `SkipTarget` 新增 `supportsRating(): Boolean`（預設 false）與 `sendRating(thumbUp: Boolean)`（預設不動作）。生產實作用 `transportControls.setRating(Rating.newThumbRating(thumbUp))`。
- `DislikeMethod` 列舉：`CUSTOM_ACTION`、`RATING`、`NONE`；`DislikeSupport.chooseMethod`。
- `SkipEvent.disliked` 與 `SkipRecord.disliked` 沿用，不改資料庫。

**Failure modes**

- `sendRating` 丟例外：記 log，照常跳過，`disliked = false`。
- 播放器接受評分但不反映（例如不影響推薦）：App 無法偵測，屬已知限制並寫入文件。

**Acceptance criteria**

- 單元測試：`chooseMethod` 三種結果；協調器評分路徑的順序（評分先於跳過）、無換歌檢查、評分未公開仍送出、例外時仍跳過、狀態在等待中改變時取消、只送一次、沒有評分支援時只跳過、自訂動作路徑不受影響（原有測試全過）。
- 模擬器：用 fake-player 的只評分模式驗證整條路徑。
- 真機路徑實測：YouTube 影片版（用抖音或 TikTok 相關影片，使用者已同意；測完取消評分）命中後倒讚按鈕變實心且影片被跳過，並量測 log 的 `latencyMs`；YouTube Music 回歸確認原路徑不變。

**Scope boundaries**

- 範圍內：上列程式、測試、fake-player、文件、版本。
- 範圍外：我不感興趣、按讚、讀取評分、改變自訂動作路徑、任何非官方 API。

## Risks / Trade-offs

- **評分是否影響推薦無法驗證**：App 只能確認播放器介面上的倒讚狀態改變，無法確認 YouTube 是否因此調整推薦。緩解：文件如實說明，介面維持「會影響帳號、無法取消」的提醒。
- **不同播放器對 `setRating` 的反應可能不同**：有的播放器可能在評分後自己換歌，造成多跳一首。緩解：目前只有 YouTube 影片版實測過；其他播放器在實測前不保證，記錄在筆記。
- **剛開始就送可能不生效**：YouTube Music 有這個現象。緩解：任務中先實測，不生效就加等待並調整常數。
- **延遲**：評分路徑的標記規則從命中到跳過約 1.5 秒（實測 `latencyMs` 1516，模擬器一次）；較快的手機可能可以更短，但未量測。
