## Context

專案目前是空的。目標是一個只做「讀取系統媒體狀態並跳歌」的 Android 開源 App(Kotlin + Jetpack Compose)。它不播放音樂,播放仍由官方 YouTube Music / YouTube / Spotify 負責,因此 Premium 免廣告與 Android Auto 都維持官方行為。服務跑在手機上,車機只是顯示端,所以車上情境不需要額外整合。

## Goals / Non-Goals

**Goals:**

- 只用官方 Android API(NotificationListenerService、MediaSessionManager、MediaController)。
- 命中規則時以 skipToNext() 跳歌,目標是在取得 metadata 後 0.5 秒內(目標值,尚未量測,見驗收)。
- 防抖動與連跳保護,不產生跳歌死循環。
- 過濾引擎為純 Kotlin,可單元測試。

**Non-Goals:**

- 不做 dislike、Shorts 隱藏、WebView、Google 登入、iOS。
- 不實作提前跳過下一首(僅做佇列探測)。
- 不翻譯使用者自己輸入的關鍵字,也不依介面語言改變預設黑名單。
- 不使用 AccessibilityService,不逆向 API,不擷取串流。
- 不保證所有播放器都會公開 metadata 或接受 skipToNext()。

## Decisions

### 以 MediaSessionManager 讀取 metadata 而非解析通知文字

NotificationListenerService 授權後,以自身 ComponentName 呼叫 `MediaSessionManager.getActiveSessions()`,並註冊 `OnActiveSessionsChangedListener` 與每個 controller 的 `MediaController.Callback.onMetadataChanged`。這比解析通知文字穩定,且能直接取得套件名稱、`METADATA_KEY_TITLE`、`METADATA_KEY_ARTIST`。

替代方案:解析通知標題與內文 → 各 App 格式不同、易壞,故僅在 session 不可用時才考慮,本次不實作。

### 以 transportControls.skipToNext 跳歌並以 KEYCODE_MEDIA_NEXT 為備援

優先呼叫命中那個 controller 的 `transportControls.skipToNext()`,只影響該播放器。僅在丟出例外時,對該 session 發送 `KEYCODE_MEDIA_NEXT` 的按下與放開事件。

替代方案:一開始就送全域按鍵 → 可能被別的播放器接收,故作為備援。

### 純 Kotlin TextFilterEngine 與內建簡體字表

`TextFilterEngine` 不依賴 Android 類別。簡體專用字以內建字表(資源檔)判斷,含日文假名即不套用簡體規則。ASCII 關鍵字用單字邊界比對,CJK 關鍵字用子字串比對。

簡體字表只收「簡體專用且日文不使用」的字元(例如 爱、们、这),日文也用的字(例如 国、学、会)不收,因此純漢字的日文標題不會被誤判;含假名或韓文 Hangul 的標題一律不套用簡體規則。代價:只使用共用字的簡體標題會漏判,這是刻意取捨。關鍵字比對先對文字與關鍵字做 Unicode NFKC 正規化,再做字面比對,不做繁簡或中日字形互轉,避免輸入簡體卻篩到日文。

替代方案:整合 OpenCC → 字典體積較大、需要原生或大型資料,對只做偵測不需要轉換的需求過重;若之後字表漏字嚴重再評估。

### 防抖動與連跳保護的狀態機

每個 session 保存「最後處理過的曲目識別(package + title + artist)」與「連續跳歌計數與時間窗」。相同識別的重複 callback 一律忽略;60 秒內連跳達 10 次即暫停並發通知,遇到未命中的曲目或 60 秒無跳歌後復原。10 次與 60 秒為設計參數,不是量測結果。

### 設定以 DataStore 儲存並以 Flow 即時提供給服務

設定(關鍵字、正規表達式、開關、受監聽播放器)存 Preferences DataStore,服務以 Flow 收集最新設定,不需重啟服務。

### 啟用總開關以開機次數判斷是否仍有效

開關狀態存在 DataStore,同時記錄開啟當下的 `Settings.Global.BOOT_COUNT`。服務只在「開關為開且記錄的開機次數等於目前開機次數」時動作,因此裝置重開機後自動視為關閉,不需要 BOOT_COMPLETED 廣播接收器。開關為關時,服務仍保持連線但忽略所有 session。

替代方案:接收 BOOT_COMPLETED 後清除旗標 → 多一個接收器與權限,且與開機次數比對相比沒有額外好處。

### 跳過紀錄以 Room 儲存並限制 1000 筆

每次發出跳歌指令就寫入一筆(時間、package、title、artist、reason)。寫入後若超過 1000 筆就刪除最舊的。1000 是設計參數,不是量測結果。清單畫面以 Flow 觀察資料庫,依受監聽播放器清單篩選。

替代方案:用 DataStore 存清單 → 不適合會成長的列表資料與查詢,故用 Room。

### 播放佇列探測只做實機傾印不做提前跳過

新增一個除錯用的探測動作,傾印所選 controller 的 `queue`、`metadata` 與 `playbackState`(含 position 與 actions),輸出到 log。依結果決定之後是否做「目前曲目結束前用 `skipToQueueItem` 跳過下一首」。本次不實作提前跳過。

### 以 AppCompat 每個 App 語言設定實作多語系

所有文字放在 `strings.xml`,並提供 `values`(英文)、`values-ko`、`values-ja`、`values-zh-rCN`、`values-zh-rTW`。語言切換使用 AndroidX `AppCompatDelegate.setApplicationLocales()`,在 Android 13 以上走系統的每個 App 語言設定,較舊版本由 AppCompat 處理並自行保存;宣告 `locales_config.xml` 讓系統設定也能看到這五種語言。選項「跟隨系統」對應空的 locale 清單。語言選單的各選項以該語言本身的名稱顯示。譯文由我撰寫初稿,需要母語者校對。

替代方案:自行管理 Configuration 並重建 Activity → 需要處理 Compose 與通知文字的重新套用,且與系統設定不同步,故不採用。

### Compose 權限導引與設定介面

主畫面顯示 Notification Access 狀態,以 `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` 開啟系統設定頁;設定頁提供關鍵字清單、簡體字開關、受監聽播放器。

## Implementation Contract

**行為**

- 授權後,受監聽播放器切到命中規則的曲目時,該播放器被跳到下一首;未命中時不送任何指令。
- 未授權時服務不運作,主畫面顯示導引。
- 啟用總開關預設關閉;開啟後離開 App、關螢幕、連車機仍運作;重開機後恢復關閉。
- 每次跳歌寫入一筆紀錄,清單畫面可依播放器篩選與清除。
- 介面可在五種語言間切換並持久化,預設跟隨系統,不支援的系統語言顯示英文。
- 探測動作把目標播放器的佇列與 metadata 印到 log。

**介面 / 資料形狀**

- `TextFilterEngine.evaluate(title: String, artist: String, config: FilterConfig): MatchResult`,其中 `MatchResult` 含 `matched: Boolean` 與 `reason: String?`(例如 `simplified-artist`、`keyword:合輯`)。
- `FilterConfig(simplifiedEnabled: Boolean, keywords: List<String>, regexes: List<String>)`。
- `MediaMonitorService : NotificationListenerService`,內部以 `TrackIdentity(pkg, title, artist)` 做防抖動。
- `SkipRecord(id, timeMillis, pkg, title, artist, reason)`(Room entity)與 `SkipHistoryDao`(insert、observe by packages、clear、trim to 1000)。
- `EnableState(enabled: Boolean, bootCount: Int)`,有效條件為 `enabled && bootCount == Settings.Global.BOOT_COUNT`。
- 時間記錄:log 一行包含 `metadataReceivedAt` 與 `skipIssuedAt` 的差值(毫秒)。

**失敗模式**

- skipToNext() 丟例外 → 改送 KEYCODE_MEDIA_NEXT;仍失敗則寫 log,不崩潰。
- 無效正規表達式 → 儲存時拒絕;已存在的壞規則評估時略過並寫 log。
- 連跳保護觸發 → 暫停跳歌並發通知。

**驗收**

- 單元測試覆蓋 text-filter-engine spec 的 Example 表所有列、預設黑名單、無效正規表達式。
- 單元測試以假的 controller 驗證:重複 metadata 只跳一次、連跳 10 次後暫停、skipToNext 例外時走備援。
- 建置檢查:`./gradlew lintDebug` 無 MissingTranslation;UI 測試切換五種語言後主畫面標題各自顯示對應語言。
- 單元測試:跳歌寫入紀錄、超過 1000 筆刪除最舊、依播放器篩選、開關在開機次數改變後視為關閉。
- 實機驗證:YouTube Music 與 Spotify 播放命中曲目被跳過;螢幕關閉仍運作;連上 Android Auto(或 Desktop Head Unit)時可跳過;實測並記錄 metadata 到跳歌的耗時,確認是否達到 0.5 秒目標。

**範圍**

- 範圍內:介面多語系與切換、過濾引擎、媒體監聽服務、Compose 設定介面、DataStore、跳過紀錄(Room)、佇列探測。
- 範圍外:提前跳過下一首、dislike、Shorts、WebView、登入、iOS、上架 Google Play 流程文件。

## Risks / Trade-offs

- [機器撰寫的韓文與日文譯文可能不自然] → 標示為初稿,上架或公開前請母語者校對。

- [播放器在曲目開始後才更新 metadata,使用者會先聽到短暫片段] → 無法避免;以記錄的耗時量測並盡量縮短處理路徑。
- [部分播放器不公開 metadata 或忽略 skipToNext] → 以 log 呈現;受監聽清單可調整。
- [簡體專用字表漏字或誤判] → 字表可擴充,以單元測試鎖定。
- [Google Play 對通知存取權的審核] → 專案定位為開源與自行安裝,上架不在本次範圍。
- [系統可能因省電模式終止背景服務] → 在主畫面提示使用者將 App 排除於電池最佳化,並在 NotificationListenerService 重新連線時重新註冊 listener。
