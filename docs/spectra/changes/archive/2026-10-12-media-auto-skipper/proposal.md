## Why

使用者在 YouTube Music、Spotify 等播放器常聽到不想要的曲目(簡體中文歌名、合輯、Mix),而這些 App 沒有提供依標題自動略過的功能。與其包裝或修改播放器(違反條款、破壞 Premium 與 Android Auto 體驗),不如做一個只讀取系統媒體狀態、並用官方媒體指令跳歌的輔助工具,車上也能照常運作。

## What Changes

- 新增開源 Android App「Media Auto-Skipper」(Kotlin + Jetpack Compose),App 本身不播放音樂。
- 新增文字過濾引擎:判斷標題或演出者是否含簡體專用字元,或命中黑名單關鍵字與正規表達式。
- 新增背景媒體監聽服務:透過 NotificationListenerService 與 MediaSessionManager 取得目前播放的套件名稱、曲名、藝人;命中規則時呼叫 `transportControls.skipToNext()`(備援為 `KEYCODE_MEDIA_NEXT`),並以防抖動機制避免重複觸發與連續跳歌死循環。
- 新增設定與權限導引介面:引導使用者開啟「通知存取權」,可編輯黑名單關鍵字、簡體字開關、受監聽的播放器清單。
- 新增「啟用」總開關:預設關閉,使用者開啟 App 並打開後才監聽;離開 App、關螢幕、連車機仍持續運作;裝置重開機後恢復為關閉。
- 新增跳過紀錄:每次跳歌都記錄時間、播放器、曲名、藝人與命中原因,可依播放器篩選與清除。
- 新增介面多語系:英文、韓文、日文、簡體中文、繁體中文,可跟隨系統或在 App 內手動切換。
- 新增一個實機探測任務:傾印 YouTube Music 與 Spotify 的播放佇列與 metadata,用來判斷「提前跳過下一首」是否可行;提前跳過本身不在本次承諾範圍。

## Non-Goals (optional)

- 不做 dislike(無法在不使用無障礙服務下操作其他 App 的按鈕),之後單獨測試。
- 不承諾提前跳過下一首(是否可行取決於探測任務的結果)。
- 不做 Shorts 隱藏、WebView、Google 登入、iOS 版本。
- 不使用 AccessibilityService、不逆向 YouTube API、不擷取任何串流。
- 不保證 0.5 秒內必定完成跳歌,僅以此為目標並提供量測方式。

## Capabilities

### New Capabilities

- `text-filter-engine`: 簡體專用字判斷(含日文假名排除)、關鍵字黑名單與正規表達式比對。
- `media-monitor-service`: 監聽啟用中的媒體 session、命中即跳歌、防抖動與連跳保護。
- `filter-settings-ui`: 通知存取權導引、啟用總開關、黑名單編輯、受監聽播放器選擇、設定持久化。
- `skip-history`: 本機儲存跳過紀錄,並提供可依播放器篩選、可清除的清單畫面。
- `ui-localization`: 介面支援英文、韓文、日文、簡體中文、繁體中文,並可在 App 內切換語言。

### Modified Capabilities

(無,專案目前沒有既有 spec)

## Impact

- 新增整個 Android 專案(Gradle、Kotlin、`app/` 模組)。
- 依賴:Jetpack Compose、AndroidX DataStore、Room(跳過紀錄);簡體字比對採內建字表(OpenCC 為替代方案,見設計文件)。
- 需要使用者手動授予「通知存取權」。
- 取代暫存中的 change `ytm-filter-android-app`(WebView 方案)。
