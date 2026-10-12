## 1. 選擇方式與介面

- [x] 1.1 在 `DislikeSupport` 新增 `DislikeMethod`（`CUSTOM_ACTION`、`RATING`、`NONE`）與 `chooseMethod(actionId, supportsRating)`：有自訂動作 id 為 `CUSTOM_ACTION`，否則支援評分為 `RATING`，否則 `NONE`。驗證：`DislikeSupportTest` 新增三種結果與「自訂動作優先於評分」的案例（對應規格需求「Dislike through the rating API when there is no dislike action」；設計決定「選擇不喜歡方式的順序」）。
- [x] 1.2 `SkipTarget` 新增 `supportsRating()`（預設 false）與 `sendRating(thumbUp)`（預設不動作）；`ControllerTarget` 以 `PlaybackState.actions` 含 `ACTION_SET_RATING` 且 `MediaController.ratingType` 為 `Rating.RATING_THUMB_UP_DOWN` 判斷支援，並以 `transportControls.setRating(Rating.newThumbRating(thumbUp))` 送出。驗證：`./gradlew :app:assembleDebug` 成功，且星等或愛心型的評分被判為不支援（對應規格需求「Official APIs only」；設計決定「『支援評分』的判斷」）。[after: 1.1]

## 2. 協調器

- [x] 2.1 `SkipCoordinator` 加入評分路徑：方法為 `RATING` 時先送倒讚評分再跳過，不套用評分未公開與已不喜歡的限制，不做換歌檢查，每首只送一次，丟例外時照常跳過且 `disliked = false`；新增常數 `RATING_SETTLE_MS`（預設 0，為 0 時同一個呼叫內完成）；方法為 `NONE` 時立刻只跳過；自訂動作路徑完全不變。驗證：`SkipCoordinatorTest` 新增案例——評分路徑順序（評分先於跳過）、評分未公開仍送出、例外仍跳過、只送一次、無評分支援只跳過、自訂動作優先、原有 36 個以上自訂動作測試全數通過（對應規格需求「Dislike through the rating API when there is no dislike action」「Wait for the player and avoid a double skip」「Never undo an existing dislike」；設計決定「評分路徑的行為」）。[after: 1.2]
- [x] 2.2 當 `RATING_SETTLE_MS` 大於 0 時，評分路徑在等待結束時重新檢查總開關、監聽清單與規則，並使用當下設定（沿用 0.2.2 做法）。驗證：`SkipCoordinatorTest` 以排程假物件新增案例——等待中關閉總開關、移除規則、移除播放器、換曲目都會取消（設計決定「等待期間重新檢查」）。[after: 2.1]

## 3. 測試播放器與實測

- [x] 3.1 `fake-player` 新增「只宣告評分」模式（啟動參數 `--ez ratingOnly true`）：不公開任何自訂動作，宣告 `ACTION_SET_RATING` 與上下拇指型評分，收到 `setRating` 時記 log 並記錄倒讚狀態。驗證：`./gradlew :fake-player:assembleDebug` 成功，模擬器上命中勾選規則後 log 顯示先收到評分、再被跳過，歷史頁出現「已不喜歡」標記。[after: 2.1]
- [x] 3.2 在真實 YouTube 影片版實測評分路徑（用抖音或 TikTok 相關影片的播放清單；使用者已同意，測完取消評分）：判斷影片剛開始就送 `setRating` 是否生效，生效則保持 `RATING_SETTLE_MS = 0`，不生效則量出可靠生效所需等待並把常數改為該值；記錄命中到跳過的 `latencyMs`，並確認被跳過那支的倒讚狀態（關閉總開關後回到該影片查看）。驗證：`docs/queue-probe-notes.md` 有實測結果與日期，常數與規格、設計文字一致，測試用的評分已取消。[after: 3.1]
- [x] 3.3 YouTube Music 回歸測試：確認自訂動作路徑行為與 0.2.2 相同（等待、換歌檢查、跳過只一首）。驗證：log 顯示 `dislike sent action=thumbs_down_action` 且沒有評分呼叫，結果寫入 `docs/queue-probe-notes.md`。[after: 2.1]

## 4. 文件與版本

- [x] 4.1 更新 `README.md`（中英文）：YouTube 影片版透過官方評分 API 也能不喜歡，並說明評分路徑的實測時間與「無法驗證是否影響推薦」；更新 `CONTRIBUTING.md` 的跳歌流程；`CHANGELOG.md` 新增 0.3.0；`app/build.gradle.kts` 版本升為 0.3.0（versionCode 6）；README 下載檔名改為 v0.3.0。驗證：`./gradlew testDebugUnitTest :filter-engine:test lintDebug :app:assembleRelease` 全部通過，README 兩個語言段落內容對應（對應規格需求「Official APIs only」）。[after: 3.2, 3.3]
