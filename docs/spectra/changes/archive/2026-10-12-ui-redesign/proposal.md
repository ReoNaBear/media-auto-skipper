## Why

目前畫面是 Material 3 的預設樣式：紫色配色、純文字分頁、設定項目平鋪在同一個捲動頁，沒有層級與分組，看起來像未完成的原型。這個 App 要公開發布，第一印象與可用性都需要改善。同時，「不喜歡」功能會讓跳過變慢、也比較不可靠（要等 1.5 到 2 秒、部分播放器會自己換歌或忽略跳過），這個後果目前只在文件裡，介面上沒有清楚告知。

## What Changes

- 套用 `penpot-uiux-design` 技能的設計原則重新設計三個分頁：品牌色（與圖示一致的青綠色）的淺色與深色主題、8 dp 網格、卡片分組、至少 48 dp 的點擊範圍、4.5:1 以上的文字對比、清楚的標題層級。
- 底部導覽加上圖示；首頁以「總開關」為視覺重心，通知存取權與電池最佳化各自成卡片；規則頁分成「偵測」「播放器」「關鍵字」「正規表達式」「語言」卡片；紀錄頁每筆為卡片並以標籤顯示原因與是否已不喜歡，空狀態加上說明。
- 規則頁的不喜歡說明改為明確警示卡：除了影響帳號推薦、無法由 App 取消之外，還要說明它會讓跳過變慢且較不可靠。五種語言同步更新。
- 程式內加註解說明不喜歡會讓跳過變慢、變得較不可預期。
- README 的截圖（首頁、規則、紀錄）全部重拍；README 與 CHANGELOG 說明同步更新。
- 版本：之前的版本號都沒有實際發布過，所以整併為 0.2.0 這一個首次公開版本（`versionName` 為 0.2.0，`versionCode` 另外遞增，避免已安裝測試版無法覆蓋安裝）。

## Non-Goals

- 不改變任何功能行為、跳過邏輯、資料或設定格式。
- 不新增畫面或新設定；不改圖示的圖形（顏色維持現有青綠色）。
- 不使用 Penpot 工具本身：它的 MCP 伺服器沒有設定，設計準則直接套用在程式碼上。

## Capabilities

### New Capabilities

（無）

### Modified Capabilities

- `filter-settings-ui`: 規則頁的不喜歡說明要求加上「會讓跳過變慢且較不可靠」的警示。

## Impact

- `app/src/main/kotlin/com/mediaautoskipper/`：新增主題檔，並改寫 `MainActivity.kt`、`ui/HomeScreen.kt`、`ui/SettingsScreen.kt`、`ui/RulesScreen.kt`、`ui/PlayersSection.kt`、`ui/HistoryScreen.kt`、`ui/LanguageSection.kt`；`app/src/main/res/drawable/` 新增導覽圖示；五種語言的 `strings.xml`。
- 既有的介面測試依賴標籤（`testTag`）與文字，需要保持相容或同步更新。
- `docs/screenshots/` 的三張截圖、`README.md`、`CHANGELOG.md`、`CONTRIBUTING.md`、`app/build.gradle.kts` 版本。
- 風險：視覺改版可能破壞既有的介面測試與無障礙語意；截圖是在模擬器上用假資料拍的。
