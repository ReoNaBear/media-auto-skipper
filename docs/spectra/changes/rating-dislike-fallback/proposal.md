## Why

0.2.x 的「不喜歡」只在播放器公開 dislike 自訂動作時有效，目前只有 YouTube Music。YouTube 影片版沒有公開自訂動作，所以勾選的規則在那裡只會跳過。實測發現 YouTube 影片版宣告支援評分（`ACTION_SET_RATING`、上下拇指型），對它呼叫官方的 `transportControls.setRating` 後倒讚按鈕變實心，再送「未評分」會變回空心。所以可以用同一組官方 API 讓 YouTube 影片版也能不喜歡，這是使用者最常用的播放器之一。

## What Changes

- 沒有 dislike 自訂動作、但宣告支援上下拇指評分的播放器，勾選規則命中時改用 `setRating(倒讚)` 送出不喜歡，再跳過。
- 選擇順序固定：有自訂動作用自訂動作（行為完全不變，仍等 2 秒、仍做 0.6 秒換歌檢查）；否則支援評分就用評分；否則只跳過。
- 評分路徑是「設定」而非「切換」，重送無害，所以不受「評分未公開就不送」限制；YouTube 不會因此自己換影片，所以評分路徑沒有換歌檢查，送出後直接跳過。
- 評分路徑的等待時間由實測決定：影片剛開始就送若生效，就立刻送出再立刻跳過（不拖慢跳過）；若不生效，量出需要等多久並以常數表示。
- 等待期間仍會重新檢查總開關、監聽清單與規則（沿用 0.2.2）。
- fake-player 新增「只宣告評分、沒有自訂動作」的模式，供沒有帳號時測試。
- README（中英文）不再說 YouTube 影片版沒有不喜歡，改為透過官方評分 API；CHANGELOG 新增 0.3.0，版本升為 0.3.0。

## Non-Goals

- 不做「我不感興趣」、不做按讚、不讀取目前評分（YouTube 的評分狀態恆為空，無法讀取）。
- 不使用 AccessibilityService、不模擬點擊、不呼叫任何非公開介面。
- 不改變自訂動作路徑（YouTube Music）的任何行為與時間常數。

## Capabilities

### New Capabilities

（無）

### Modified Capabilities

- `dislike-on-skip`: 新增「沒有自訂動作時改用評分 API」；修改三條既有需求：只用官方 API 的說明（原本只允許自訂動作）、「不取消既有不喜歡」只適用自訂動作路徑、等待與避免重複跳過的說明（評分路徑不等換歌檢查）。

## Impact

- `app/src/main/kotlin/com/mediaautoskipper/monitor/`：`SkipTarget` 介面新增支援評分與送出評分的成員；`DislikeSupport` 新增選擇方法的純函式；`SkipCoordinator` 新增評分路徑；`MediaMonitorService` 的 `ControllerTarget` 實作。
- `fake-player/`：新增只宣告評分的模式。
- 單元測試：`SkipCoordinatorTest`、`DislikeSupportTest`。
- 文件：`README.md`、`CHANGELOG.md`、`CONTRIBUTING.md`、`docs/queue-probe-notes.md`、`app/build.gradle.kts` 版本。
- 風險：評分 API 對 YouTube 推薦的實際影響無法驗證；不同播放器對 `setRating` 的行為可能不同；評分會寫入使用者帳號，本 App 無法取消。
