# Changelog / 更新紀錄

## 0.1.0 (2026-10-12) — first release / 第一版

- Skip tracks whose title or artist contains Simplified-Chinese-only characters, a keyword, or a regular expression. / 依簡體中文專有字、關鍵字或正規表達式跳過曲目。
- Works through media sessions of YouTube Music, YouTube and Spotify; other media apps can be added. / 透過 YouTube Music、YouTube、Spotify 的媒體工作階段運作,可新增其他媒體 App。
- Master switch (off by default, resets after reboot), debounce and loop protection. / 總開關(預設關閉、重開機後重置)、防抖動與連跳保護。
- Skip history stored on the device, filterable and clearable. / 跳過紀錄只存在本機,可篩選與清除。
- UI in English, 한국어, 日本語, 简体中文, 繁體中文 with an in-app language switch. / 介面支援五種語言,可在 App 內切換。
- Battery-optimization hint on the home screen. / 主畫面提供電池最佳化提示。

Tested / 已測試: emulator (API 37) and a Sharp R11 phone with YouTube and YouTube Music. / 模擬器（API 37）與 Sharp R11 手機的 YouTube、YouTube Music。

Known limits / 已知限制: real-car Android Auto and real Spotify are not verified; YouTube may not accept "next" for single videos (not confirmed on the phone either way); ko/ja translations need native review; the Simplified-character table is hand-written. / 尚未驗證實車 Android Auto、真實 Spotify 與實體手機;YouTube 單支影片可能不接受「下一首」;韓日文翻譯待母語者校對;簡體字表為手寫。
