## 1. 主題與共用元件

- [x] 1.1 新增 `MasTheme`（青綠品牌色、淺色與深色、8 dp 網格與卡片圓角 12 dp 的共用常數）與共用元件（區塊標題、卡片），並在 `MainActivity` 套用；底部導覽三項加上自繪向量圖示（首頁、規則、紀錄）。驗證：`./gradlew :app:assembleDebug` 成功，模擬器淺色與深色下導覽列各項有圖示且選取狀態可辨識（設計決定「設計準則與 token」「品牌色與主題」「版面」）。

## 2. 三個分頁

- [x] 2.1 改寫首頁：品牌區、總開關大卡片（整列可點、狀態文字、保留 `Tags.ENABLE_SWITCH`）、通知存取權狀態卡（已授權與未授權兩種樣式，未授權保留主要按鈕與說明）、電池最佳化提示卡。驗證：`HomeContentTest` 全部通過，模擬器截圖三種狀態（未授權、已授權且關閉、已授權且開啟）可辨識、文字無截斷（設計決定「版面」）。[after: 1.1]
- [x] 2.2 改寫規則頁：依區塊分成卡片（偵測、播放器、關鍵字、正規表達式、語言），保留 `RuleTags`、`PlayerTags` 與各函式簽名，不喜歡選項改為每條規則下方的小選項，並在規則區頂部顯示不喜歡警示卡。驗證：`RuleSectionTest`、`PlayersSectionTest`、`LocalizationTest` 全部通過，截圖可見警示卡（對應規格需求「Per-rule dislike option」；設計決定「不喜歡警示文字」）。[after: 1.1]
- [x] 2.3 改寫紀錄頁：可橫向捲動的篩選晶片、清除按鈕、每筆為卡片並以標籤顯示原因與已不喜歡，空狀態加圖示與說明，保留 `HistoryTags`。驗證：`HistoryContentTest` 全部通過，截圖有紀錄與空狀態兩種畫面（設計決定「版面」）。[after: 1.1]

## 3. 文字與程式說明

- [x] 3.1 把 `dislike_note` 改成三句版（送出不喜歡、影響帳號且無法取消、讓跳過變慢且較不可靠），五種語言同步，並新增導覽與空狀態用到的字串；在 `SkipCoordinator`、`AppSettings.dislikeRules` 與規則頁相關程式加註解說明不喜歡會讓跳過變慢且較不可預期。驗證：`./gradlew lintDebug` 無 MissingTranslation，`PlayersSectionTest` 的 "cannot undo it" 斷言仍通過，`LocalizationTest` 通過（對應規格需求「Per-rule dislike option」；設計決定「不喜歡警示文字」）。[after: 2.2]

## 4. 截圖、文件與版本

- [x] 4.1 在模擬器用假播放器產生資料，重拍首頁、規則、紀錄三張英文淺色截圖並取代 `docs/screenshots/` 內的舊圖；目視檢查淺色與深色各一次是否有截斷或對比不足。驗證：三張新截圖已存檔且是新版畫面，`docs/queue-probe-notes.md` 或截圖旁註明拍攝環境（設計決定「測試相容」）。[after: 2.1, 2.2, 2.3, 3.1]
- [x] 4.2 更新 `README.md`（中英文）說明不喜歡會讓跳過變慢且較不可靠、截圖與說明對應新畫面；`CHANGELOG.md` 整併為單一 0.2.0（首次公開版本）；`CONTRIBUTING.md` 與下載檔名改為 v0.2.0；`app/build.gradle.kts` 的 `versionName` 為 0.2.0、`versionCode` 遞增為 7。驗證：`./gradlew testDebugUnitTest :filter-engine:test lintDebug :app:assembleRelease` 全部通過，簽章後 `apksigner verify` 通過，`aapt2 dump badging` 顯示 versionName 0.2.0。[after: 4.1]
