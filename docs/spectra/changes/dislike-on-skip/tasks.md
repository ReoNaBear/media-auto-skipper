## 1. 探測

- [x] 1.1 在 YouTube 影片版上執行除錯探測（`adb shell am broadcast -a com.mediaautoskipper.PROBE -p com.mediaautoskipper`，輸出已含 `ratingType`、`userRating` 與 `customActions`），把「是否公開 dislike 動作、評分型別、使用者評分狀態」寫進 `docs/queue-probe-notes.md`。驗證：筆記檔中 YouTube 一列有實測結果與日期，並說明若沒有公開 dislike 動作則該播放器只會跳過。

## 2. 設定

- [x] 2.1 `AppSettings` 新增 `dislikeRules: Set<String>`（規則鍵 `simplified`、`keyword:<文字>`、`regex:<運算式>`），`SettingsStore` 以 DataStore 項目 `dislike_rules` 讀寫；項目從未寫入時為空集合。驗證：`SettingsStoreTest` 新增案例——未寫入時所有規則都是「只跳過」、寫入後重開 DataStore 仍在。（對應規格需求「Per-rule dislike option」「Existing settings stay skip only」；設計決定「每條規則的旗標以「規則鍵」存放在獨立的 DataStore 項目」）
- [x] 2.2 規則編輯時旗標跟著動：編輯規則文字時旗標移到新文字，刪除規則時旗標移除，之後同文字新增的規則旗標為關；讀取時找不到對應規則的鍵被忽略。驗證：`RuleEditsTest`／`SettingsStoreTest` 新增「編輯保留」「刪除清除並重新新增為關」「殘留鍵不影響」三個案例。[after: 2.1]（對應規格需求「Dislike option follows rule edits」）

## 3. 協調器與播放器

- [x] 3.1 新增不依賴 Android 物件的純函式：從自訂動作清單（action id 與 name）找出 dislike 動作（id 含 `thumbs_down` 或 `dislike` 優先，其次 name 等於 Dislike，皆不分大小寫），以及把使用者評分轉成 `RatingState`（UNKNOWN／UNRATED／LIKED／DISLIKED）。驗證：單元測試涵蓋規格中的三個辨識案例（id 命中、只有 name 命中、都沒有）與四種評分狀態。（對應規格需求「Recognize the player's dislike action」「Never undo an existing dislike」；設計決定「辨識 dislike 動作的方式」「以使用者評分避免「取消」」）
- [x] 3.2 `SkipTarget` 新增 `dislikeActionId()`、`ratingState()`、`sendCustomAction(id)`；`SkipCoordinator` 在規則命中且通過防抖動與連跳保護後，若該規則鍵在 `dislikeRules`、有動作且評分為 UNRATED 或 LIKED，就先送出再跳過；例外只記 log 並照常跳過；`SkipEvent` 新增 `disliked`。驗證：`SkipCoordinatorTest` 新增案例——旗標開、旗標關、無動作、已不喜歡、評分未公開、先前按讚、送出丟例外、總開關關閉、重複 metadata、連跳暫停；並斷言送出順序為 dislike 先於 skip、同一首最多送一次。[after: 2.1, 3.1]（對應規格需求「Send dislike before skipping for opted-in rules」「Fall back to skip only without error」「Dislike only when a skip would happen」；設計決定「每首最多一次，且只在真的會跳過時送出」「命中後先送 dislike，再跳過」）

## 4. 紀錄

- [x] 4.1 `SkipRecord` 新增 `disliked: Boolean`，Room 資料庫版本 1→2，migration 為 `ALTER TABLE skip_record ADD COLUMN disliked INTEGER NOT NULL DEFAULT 0`；舊紀錄保留且視為未送出。驗證：`SkipHistoryTest` 新增案例——先用舊版結構建立含紀錄的資料庫檔案，升級後紀錄仍在且 `disliked` 為 false；新紀錄寫入 true 後能讀回。（對應規格需求「Record whether a dislike was sent」；設計決定「紀錄資料庫升版」）

## 5. 介面

- [x] 5.1 規則頁每條關鍵字與正規表達式下方加「同時標為不喜歡」勾選框，簡體中文開關下方加同樣勾選框（開關關閉時停用），規則頁顯示一行「會影響帳號推薦且本 App 無法取消」的說明；切換時寫入 `dislikeRules`。驗證：`RuleSectionTest` 與設定頁測試新增案例——預設未勾、可切換、簡體開關關閉時勾選框停用、說明文字出現。[after: 2.2]
- [x] 5.2 歷史頁在 `disliked` 為 true 的紀錄上顯示標記文字，其餘紀錄不顯示。驗證：`HistoryContentTest` 新增有標記與無標記各一筆的案例。[after: 4.1]
- [x] 5.3 五種語言（en、ko、ja、zh-CN、zh-TW）補齊勾選框標籤、帳號影響說明與歷史標記的字串。驗證：`./gradlew lintDebug` 無 MissingTranslation，且 `LocalizationTest` 仍通過。[after: 5.1, 5.2]

## 6. 串接與驗證

- [x] 6.1 `MediaMonitorService` 的 `ControllerTarget` 以 `playbackState.customActions`、`metadata` 的 `USER_RATING` 與 `transportControls.sendCustomAction(id, null)` 實作新增的三個成員，並把 `SkipEvent.disliked` 寫進 Room；`fake-player` 加入一個 Dislike 自訂動作與評分狀態，方便沒有帳號時測試。驗證：`./gradlew :app:assembleDebug :fake-player:assembleDebug` 成功，且在模擬器上用 fake-player 命中已勾選的規則後，log 顯示先送出動作再跳過，歷史頁出現標記。[after: 3.2, 4.1]（對應規格需求「Official APIs only」：送出時使用的 action id 取自播放器自己公開的清單）
- [x] 6.2 在真實 YouTube Music 與 YouTube 上各驗證一次（先取得使用者同意，因為會改動真實帳號的評分；用不重要的曲目測試，測完請使用者在 App 內把拇指取消）：命中已勾選規則時畫面上的不喜歡狀態改變且曲目被跳過，對已不喜歡的曲目不會取消，並量測 log 的 `latencyMs`。把結果與 App 版本寫進 `docs/queue-probe-notes.md`。驗證：筆記檔有實測結果；測試用的紀錄已在 App 內清除。[after: 1.1, 5.3, 6.1]
- [x] 6.3 更新文件與版本：`README.md`（中英文）把「不喜歡」從範圍外移到功能說明，並註明「我不感興趣」不做的原因與帳號影響；`CONTRIBUTING.md` 的跳歌流程加入 dislike 步驟；`CHANGELOG.md` 新增版本；`app/build.gradle.kts` 版本升為 0.2.0。驗證：`./gradlew testDebugUnitTest :filter-engine:test lintDebug :app:assembleDebug` 全部通過，README 兩個語言段落內容對應一致。[after: 6.2]
