## Why

目前命中規則的曲目只會被跳過，但播放器的推薦演算法不知道使用者不想聽這類曲目，之後還是會一直推薦。YouTube Music 在媒體工作階段（media session）裡公開了官方的「不喜歡」動作（實測 custom action `thumbs_down_action`、評分型別為上下拇指），所以可以在跳過前順手告訴播放器「這首我不要」，讓推薦逐漸減少。使用者也希望能依規則決定要不要這麼做，例如簡體標題一律不喜歡，但「Mix」只跳過就好。

## What Changes

- 每一條規則（每個關鍵字、每個正規表達式，以及「簡體中文標題」開關）各自有一個選項：只跳過（預設，行為與現在相同）或「不喜歡後跳過」。
- 命中且「不喜歡後跳過」的規則，先用 `MediaController.transportControls.sendCustomAction` 送出播放器自己公開的 dislike 動作，再照現有流程跳到下一首。
- 播放器沒有公開 dislike 動作，或該曲目已經是不喜歡時，不送出，直接只跳過，不報錯。
- 跳過紀錄新增「是否已送出不喜歡」欄位，歷史頁每筆紀錄顯示該標記。
- 規則頁每條規則與簡體開關旁加上選項，並有一行說明「會影響你帳號的推薦」；五種語言文案補齊。
- 儲存格式向下相容：既有關鍵字與正規表達式一律視為「只跳過」；紀錄資料庫升級一個版本，舊紀錄的欄位視為未送出。
- README、CONTRIBUTING 與 `docs/queue-probe-notes.md` 同步更新：「不喜歡」不再列為範圍外，「我不感興趣」仍不做並註明原因。

## Non-Goals

- 不做「我不感興趣（not interested）」：實測 YouTube Music 只公開 Like、Dislike、Shuffle、Repeat 四個自訂動作，沒有對應動作。
- 不使用 AccessibilityService、不逆向任何 App 的 API、不模擬點擊畫面按鈕；只使用播放器自己在媒體工作階段公開的動作。
- 不做「按讚（like）」，也不做批次對既有曲目補送不喜歡。
- 不新增全域的「不喜歡」總開關；預設每條規則都是「只跳過」。

## Capabilities

### New Capabilities

- `dislike-on-skip`: 命中的曲目依規則設定先送出播放器公開的 dislike 動作再跳過，包含動作的辨識、已是不喜歡時不重複送出（避免取消）、每首最多一次，以及失敗時退回只跳過。

### Modified Capabilities

- `filter-settings-ui`: 每條規則與簡體開關新增「不喜歡後跳過」選項，並持久化、向下相容。
- `skip-history`: 跳過紀錄新增「是否已送出不喜歡」欄位並在歷史頁顯示。

## Impact

- `filter-engine/`：不需變更；現有比對結果的 `reason` 字串即可對應到規則。
- `app/src/main/kotlin/com/mediaautoskipper/monitor/`：`SkipCoordinator`、`SkipTarget` 介面與 `MediaMonitorService` 的 `ControllerTarget`（新增送出自訂動作與讀取使用者評分）。
- `app/src/main/kotlin/com/mediaautoskipper/settings/`：`AppSettings`、`SettingsStore`、`RuleEdits`（新增每條規則的不喜歡旗標，編輯時旗標隨規則移動、刪除時一併移除）。
- `app/src/main/kotlin/com/mediaautoskipper/history/`：`SkipRecord` 新增欄位，Room 資料庫版本 1→2 並加入 migration。
- `app/src/main/kotlin/com/mediaautoskipper/ui/`：規則頁、簡體開關列、歷史頁；`app/src/main/res/values*/strings.xml` 五種語言。
- `fake-player/`：加入 Dislike 自訂動作，供沒有帳號時測試。
- 文件：`README.md`、`CONTRIBUTING.md`、`docs/queue-probe-notes.md`、`docs/spectra/specs/` 內的相關規格。
- 風險：不喜歡會改變使用者 Google 帳號的推薦與評分紀錄，且無法由本 App 清除，因此預設關閉並在介面上說明。
