# ui-localization Specification

## Purpose

Present the app interface in English, Korean, Japanese, Simplified Chinese and Traditional Chinese, with a language switch inside the app. Localization affects only the interface text and does not change filter rules.

## Requirements

### Requirement: Supported interface languages

The app SHALL provide a complete interface translation for English, Korean, Japanese, Simplified Chinese and Traditional Chinese. Every user-visible string in the app, including notifications and error messages, SHALL come from translatable resources.

#### Scenario: All languages complete

- **WHEN** the app is built
- **THEN** the build reports no missing translation for any of the five languages

---
### Requirement: In-app language selection

The app SHALL provide a language setting with the options "Follow system", English, 한국어, 日本語, 简体中文 and 繁體中文. Each option SHALL be labeled in its own language. Choosing an option SHALL change the interface language immediately without restarting the device, and the choice SHALL persist across restarts.

#### Scenario: Switch to Japanese

- **WHEN** the user selects 日本語 in the language setting
- **THEN** the interface text is shown in Japanese immediately

#### Scenario: Choice persists

- **WHEN** the user selects 한국어 and restarts the app
- **THEN** the interface is still in Korean

---
### Requirement: Default language

On first run the language setting SHALL be "Follow system". When the system language is not one of the five supported languages, the app SHALL show English.

#### Scenario: Unsupported system language

- **WHEN** the system language is French and the setting is "Follow system"
- **THEN** the interface is shown in English

##### Example: system language mapping

| System language | Setting | Interface |
|-----------------|---------|-----------|
| zh-TW | Follow system | Traditional Chinese |
| zh-CN | Follow system | Simplified Chinese |
| ja | Follow system | Japanese |
| fr | Follow system | English |
| ja | English | English |

---
### Requirement: Localization is independent of filtering

Changing the interface language SHALL NOT change the keyword list, the default blacklist entries, or any filter result.

#### Scenario: Keywords unchanged after switching language

- **WHEN** the user has keyword "Nightcore" and switches the interface from English to Korean
- **THEN** the keyword list still contains "Nightcore" and filtering results are unchanged
