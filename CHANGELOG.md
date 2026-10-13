# Changelog / 更新紀錄

## 0.2.1 (2026-10-13)

- Screen reader (TalkBack): the Edit, Delete, Remove and Add buttons now announce which rule or player they belong to, and input errors are read out when they appear. / 螢幕閱讀器（TalkBack）：編輯、刪除、移除、新增按鈕現在會念出所屬的規則或播放器，輸入錯誤出現時也會念出來。
- Korean and Japanese wording polished (Japanese now uses full-width punctuation). / 修飾韓文與日文用字（日文改用全形標點）。
- README lists the tested players (YouTube Music, YouTube, Spotify, KKBOX work; MixerBox does not, because it ignores the "next" command from other apps). / README 列出已測試的播放器（YouTube Music、YouTube、Spotify、KKBOX 可用；MixerBox 不可用，因為它忽略其他 App 送的「下一首」指令）。
- Added an Android Auto test checklist in `docs/`. / 在 `docs/` 新增 Android Auto 實車測試清單。
- Skipping behaviour is unchanged. / 跳過行為沒有改變。

## 0.2.0 (2026-10-12) — first public release / 首次公開版本

Earlier internal build numbers (0.1.x up to 0.3.0) were never published, so they are folded into this release. / 先前的內部版本號（0.1.x 到 0.3.0）都沒有發布過，已整併成這一個版本。

**Skipping / 跳過**

- Skips tracks whose title or artist contains Simplified-Chinese-only characters, a keyword or a regular expression, on YouTube Music, YouTube and Spotify; other media apps can be added. / 依簡體中文專有字、關鍵字或正規表達式，跳過 YouTube Music、YouTube、Spotify 的曲目，可新增其他媒體 App。
- The Simplified-character table is generated from Unicode Unihan, GB2312, Big5 and JIS X 0208 (about 2,000 characters), and spares characters that are valid in Traditional Chinese or Japanese. / 簡體字表由 Unicode Unihan、GB2312、Big5、JIS X 0208 自動產生（約 2,000 字），不誤判繁體或日文也有效的字。
- Master switch (off by default, resets after reboot), debounce, and loop protection (20 skips per 60 seconds). / 總開關（預設關閉、重開機後重置）、防抖動，以及連跳保護（60 秒 20 次）。
- Works with the screen off; checked on an emulator and on a Sharp R11 phone (Android 16) with YouTube and YouTube Music. / 螢幕關閉時可運作；已在模擬器與 Sharp R11（Android 16）以 YouTube、YouTube Music 驗證。

**"Also mark as dislike" / 同時標為不喜歡**

- Optional, off by default, set per rule (every keyword, every regular expression and the Simplified Chinese rule). A marked match sends the player's dislike, then skips. / 選用、預設關閉、每條規則各自設定（每個關鍵字、每個正規表達式與簡體中文規則）。命中時先對播放器送出不喜歡，再跳過。
- YouTube Music uses its own dislike action; the YouTube video app uses the official thumbs-down rating call; a player with neither only skips. / YouTube Music 用它自己的不喜歡動作；YouTube 影片版用官方的倒讚評分呼叫；兩者都沒有的播放器只會跳過。
- **It makes skipping slower and less reliable**: a marked match waits about 2 seconds (YouTube Music) or 1.5 seconds (YouTube video app) before acting, some players move on by themselves, and a skip sent while a video is still loading can be ignored (it is retried). Plain rules are not affected. These times were measured on an emulator, one session each. / **它會讓跳過變慢、變得較不可靠**：勾選的規則命中後要先等約 2 秒（YouTube Music）或 1.5 秒（YouTube 影片版）才動作，有些播放器會自己換歌，影片還在載入時送出的跳過可能被忽略（會重試）。沒勾選的規則不受影響。這些時間只在模擬器上各量一次。
- The dislike is written to your account and changes your recommendations; the app cannot undo it. Whether YouTube counts the rating like a tap on the thumb cannot be verified. / 不喜歡會寫進你的帳號並影響推薦，本 App 無法取消；YouTube 是否把評分當成按下倒讚，無法驗證。
- The history shows a "Disliked" label on those skips. / 紀錄頁會在這些跳過上顯示「已不喜歡」標籤。

**Interface / 介面**

- New design: teal brand colour with light and dark themes, cards, icons in the bottom bar, 48 dp touch targets, and a clear warning card about the dislike option. Designed with the `penpot-uiux-design` guidelines. / 全新設計：青綠品牌色、淺色與深色主題、卡片、底部導覽圖示、48 dp 點擊範圍，以及不喜歡選項的警示卡。依 `penpot-uiux-design` 準則設計。
- English, 한국어, 日本語, 简体中文, 繁體中文 with an in-app language switch. / 支援五種語言，可在 App 內切換。
- Battery-optimization hint on the home screen. / 主畫面提供電池最佳化提示。
- Skip history stored on the device only, filterable and clearable. / 跳過紀錄只存在本機，可篩選與清除。

**Not verified yet / 尚未驗證**: real-car Android Auto; real Spotify; the reboot reset on a physical phone; ko/ja translations need native review. / 實車 Android Auto、真實 Spotify、實體手機上的重開機重置；韓日文翻譯待母語者校對。
