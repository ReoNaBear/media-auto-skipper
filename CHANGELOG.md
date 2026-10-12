# Changelog / 更新紀錄

## 0.1.1 (2026-10-12)

- Loop protection raised from 10 to 20 skips per 60 seconds: on a phone, 10 matching videos in a row stopped skipping. / 連跳保護從 60 秒 10 次調高到 20 次：實機上連續 10 部符合規則的影片就被停下來。
- Verified on a Sharp R11 (Android 16) with YouTube and YouTube Music, including skipping with the screen off. / 已在 Sharp R11（Android 16）以 YouTube、YouTube Music 驗證，包含螢幕關閉時跳歌。

## 0.1.0 (2026-10-12) — first release / 第一版

- Skip tracks whose title or artist contains Simplified-Chinese-only characters, a keyword, or a regular expression. / 依簡體中文專有字、關鍵字或正規表達式跳過曲目。
- Works through media sessions of YouTube Music, YouTube and Spotify; other media apps can be added. / 透過 YouTube Music、YouTube、Spotify 的媒體工作階段運作,可新增其他媒體 App。
- Master switch (off by default, resets after reboot), debounce and loop protection. / 總開關(預設關閉、重開機後重置)、防抖動與連跳保護。
- Skip history stored on the device, filterable and clearable. / 跳過紀錄只存在本機,可篩選與清除。
- UI in English, 한국어, 日本語, 简体中文, 繁體中文 with an in-app language switch. / 介面支援五種語言,可在 App 內切換。
- Battery-optimization hint on the home screen. / 主畫面提供電池最佳化提示。

Known limits / 已知限制: real-car Android Auto and real Spotify are not verified; YouTube may not accept "next" for single videos (not confirmed on the phone either way); ko/ja translations need native review; the Simplified-character table is hand-written. / 尚未驗證實車 Android Auto、真實 Spotify 與實體手機;YouTube 單支影片可能不接受「下一首」;韓日文翻譯待母語者校對;簡體字表為手寫。
