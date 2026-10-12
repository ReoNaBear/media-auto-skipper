## 1. 專案骨架

- [x] 1.1 建立 Android Gradle 專案(Kotlin、Jetpack Compose、minSdk 26、`app/` 模組)與單元測試設定。完成時 `./gradlew assembleDebug` 與 `./gradlew testDebugUnitTest` 皆成功。

## 2. 過濾引擎

- [x] 2.1 依「純 Kotlin TextFilterEngine 與內建簡體字表」決策實作 Simplified Chinese detection:字表只含日文不使用的簡體字,含假名或 Hangul 的標題不套用。驗證:單元測試涵蓋 spec Example 表八列,包含 國境 不命中、爱 命中、含 Hangul 不命中。[after: 1.1]
- [x] 2.2 實作 Keyword blacklist matching 與 Keyword language independence:NFKC 正規化後做字面比對,CJK、韓文、假名為子字串,ASCII 為單字邊界,不做繁簡互轉。驗證:單元測試涵蓋 keyword Example 表十二列,包含 "Mix" 不命中 "Remix"、合辑 不命中 經典合輯、關鍵字 爱 不命中 愛のうた。[after: 2.1]
- [x] 2.3 實作 Regular expression rules 與 Default blacklist:無效正規表達式儲存時被拒絕,預設清單為六項且可移除。驗證:單元測試輸入 `([` 被拒絕,首次執行清單恰為六項。[after: 2.2]
- [x] 2.4 實作 Evaluation covers title and artist:回傳 `MatchResult` 含原因。驗證:單元測試標題 "Song"、藝人 "华语群星" 回傳原因 `simplified-artist`。[after: 2.3]

## 3. 設定與啟用狀態

- [x] 3.1 依「設定以 DataStore 儲存並以 Flow 即時提供給服務」決策實作 Settings persistence。驗證:單元測試寫入後重建 DataStore 仍讀回相同設定,且預設受監聽播放器為 YouTube Music、YouTube、Spotify。[after: 1.1]
- [x] 3.2 依「啟用總開關以開機次數判斷是否仍有效」決策實作 Master enable switch 的狀態:預設關閉,記錄開啟時的開機次數,開機次數改變即視為關閉。驗證:單元測試涵蓋預設關閉、開啟後有效、模擬開機次數改變後無效。[after: 3.1]

## 4. 媒體監聽服務

- [x] 4.1 依「以 MediaSessionManager 讀取 metadata 而非解析通知文字」決策實作 Observe active media sessions 與 Behavior without permission,服務在開關為關時忽略所有 session,並在 manifest 宣告服務與權限。驗證:實機授權後開關開啟時切歌 log 出套件名稱、曲名、藝人;開關關閉或未授權時不動作。[after: 3.2]
- [x] 4.2 依「以 transportControls.skipToNext 跳歌並以 KEYCODE_MEDIA_NEXT 為備援」決策實作 Skip matching tracks 並記錄 metadata 到跳歌的毫秒耗時。驗證:假 controller 單元測試涵蓋命中跳過、未命中不動作、skipToNext 例外時走備援。[after: 4.1, 2.4]
- [x] 4.3 依「防抖動與連跳保護的狀態機」決策實作 Debounce repeated metadata 與 Skip loop protection。驗證:單元測試重複三次相同 metadata 只跳一次;連跳 10 次後暫停並發通知,遇到未命中曲目後復原。[after: 4.2]
- [x] 4.4 依「跳過紀錄以 Room 儲存並限制 1000 筆」決策實作 Local skip history storage 與 Record each skip:每次跳歌(含備援)寫入一筆,超過 1000 筆刪除最舊。驗證:單元測試跳歌後出現一筆含正確欄位的紀錄,寫入第 1001 筆後剩 1000 筆。[after: 4.2]
- [x] 4.5 驗證 Background and car operation 與 Master enable switch 的背景行為:開關開啟後離開 App、螢幕關閉、連接 Android Auto(或 Desktop Head Unit)時命中曲目被跳過,重開機後開關為關。驗證:在 YouTube 與 YouTube Music(必測)及 Spotify 上手動測試並記錄各自的 log 與耗時,確認是否達到 0.5 秒目標。[after: 4.3, 4.4]
- [x] 4.6 依「播放佇列探測只做實機傾印不做提前跳過」決策實作除錯用探測動作,傾印所選 controller 的 queue、metadata、playbackState。驗證:在 YouTube、YouTube Music 與 Spotify 各執行一次(YouTube 與 YouTube Music 為必測項目),將輸出結果與結論(佇列是否可讀、是否含下一首標題)記錄在 `docs/` 下的筆記檔,作為是否做提前跳過的依據。[after: 4.1]

## 5. 設定與紀錄介面

- [x] 5.1 依「Compose 權限導引與設定介面」決策實作 Notification Access guidance 與 Enable switch control。驗證:UI 測試未授權時顯示說明且開關無法開啟,授權後可開啟並顯示背景持續運作的提醒。[after: 3.2]
- [x] 5.2 實作 Blacklist keyword editing:新增、編輯、刪除關鍵字與正規表達式,空白或無效項目顯示錯誤。驗證:UI 測試輸入空白字串後清單不變,輸入 "Nightcore" 後出現在清單。[after: 5.1, 2.3]
- [x] 5.3 實作 Rule and player toggles:簡體字開關與受監聽播放器清單,可從已安裝媒體 App 新增。驗證:UI 測試關閉簡體規則後僅命中該規則的曲目不再被跳過。[after: 5.2, 4.2]
- [x] 5.4 實作 Skip history list screen、Filter history by player 與 Clear history:新到舊清單、沿用受監聽播放器清單篩選、確認後清除。驗證:UI 測試 10:05 的紀錄在 10:00 之上、只選 Spotify 時僅列出 Spotify、取消清除時紀錄保留、確認清除後顯示空狀態。[after: 4.4, 5.3]
- [x] 5.5 依「以 AppCompat 每個 App 語言設定實作多語系」決策實作 Supported interface languages、In-app language selection、Default language 與 Localization is independent of filtering:五種語言的 `strings.xml`、語言選單、`locales_config.xml`、持久化。驗證:`./gradlew lintDebug` 無 MissingTranslation;UI 測試切換日文後主畫面立即顯示日文,重啟後仍為韓文;系統語言為法文時顯示英文;切換語言後關鍵字清單不變。[after: 5.4]
