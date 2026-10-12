# text-filter-engine Specification

## Purpose

Decide whether a track title or artist name matches the user's exclusion rules. The engine is a pure function layer with no Android dependency so it is fully unit-testable.

## Requirements

### Requirement: Simplified Chinese detection

When the Simplified Chinese rule is enabled, the engine SHALL treat a text as matching if it contains at least one character from a built-in set of characters that exist only in Simplified Chinese. A text that contains Japanese kana (Hiragana or Katakana) SHALL NOT match through this rule.

#### Scenario: Simplified-only character matches

- **WHEN** the rule is enabled and the text is "爱你"
- **THEN** the engine reports a match

#### Scenario: Traditional text does not match

- **WHEN** the rule is enabled and the text is "愛你"
- **THEN** the engine reports no match

##### Example: detection cases

| Text | Rule | Matches | Notes |
|------|------|---------|-------|
| 爱你 | on | yes | 爱 is Simplified-only |
| 愛你 | on | no | Traditional |
| 的 | on | no | shared character |
| 恋するフォーチュン国 | on | no | contains kana |
| 국경의 밤 爱 | on | no | contains Hangul |
| 国境 | on | no | 国 is shared with Japanese, not in the set |
| 爱 | on | yes | 爱 is not used in Japanese (Japanese uses 愛) |
| 爱你 | off | no | rule disabled |

---
### Requirement: Keyword blacklist matching

The engine SHALL match a text against a keyword blacklist case-insensitively. A keyword made of CJK characters SHALL match as a substring. A keyword made only of ASCII letters, digits or spaces SHALL match only on word boundaries, so that "Mix" matches "Summer Mix" but does not match "Remix". Blank keywords SHALL be ignored. The engine SHALL normalize both the text and the keywords with Unicode NFKC before comparing, so that full-width and half-width forms and composed and decomposed Hangul compare equal. The engine SHALL compare keywords literally and SHALL NOT convert between Simplified and Traditional Chinese or between Chinese and Japanese forms. Keywords in Korean, Japanese kana and other non-ASCII scripts SHALL match as substrings, and the Simplified Chinese exemptions for kana and Hangul SHALL NOT apply to keyword matching.

#### Scenario: CJK keyword matches as substring

- **WHEN** the blacklist contains "合輯" and the text is "經典合輯 Vol.2"
- **THEN** the engine reports a match

#### Scenario: ASCII keyword requires word boundary

- **WHEN** the blacklist contains "Mix" and the text is "Love (Remix)"
- **THEN** the engine reports no match

##### Example: keyword cases

| Keyword | Text | Matches | Notes |
|---------|------|---------|-------|
| Mix | Summer Mix 2024 | yes | whole word |
| mix | SUMMER MIX | yes | case-insensitive |
| Mix | Remix | no | inside a word |
| Full Album | Artist - Full Album | yes | multi-word |
| 精选 | 华语精选 | yes | CJK substring |
| (blank) | anything | no | ignored |
| ＭＩＸ | Summer Mix | yes | full-width normalized |
| 合辑 | 经典合輯 | no | literal, no Simplified-Traditional conversion |
| 合輯 | 經典合輯 | yes | literal |
| 베스트 | 2024 베스트 모음 | yes | Korean substring |
| ベスト | ベスト盤 | yes | Japanese kana substring |
| 国 | 国境 | yes | the user chose this keyword explicitly |

---
### Requirement: Keyword language independence

A keyword the user enters in any language SHALL be applied to all titles regardless of the interface language. A keyword SHALL NOT cause matches in titles that do not contain that keyword literally after normalization.

#### Scenario: Simplified keyword does not hit Japanese title

- **WHEN** the blacklist contains "爱" and the title is "愛のうた"
- **THEN** the engine reports no match

#### Scenario: Interface language does not change matching

- **WHEN** the interface language changes from English to Korean
- **THEN** the same title and keywords produce the same match result

---
### Requirement: Regular expression rules

The engine SHALL support user-defined regular expression rules evaluated case-insensitively against the text. An invalid expression SHALL be rejected when saved and SHALL NOT crash evaluation of other rules.

#### Scenario: Regex matches

- **WHEN** the rule `^\[.*Live.*\]` exists and the text is "[Live] Song"
- **THEN** the engine reports a match

#### Scenario: Invalid regex rejected

- **WHEN** the user saves the expression `([`
- **THEN** the save is rejected with an error and the rule list is unchanged

---
### Requirement: Default blacklist

The engine SHALL ship with a default blacklist containing 合輯, 合辑, 精選, 精选, Full Album and Mix. The user SHALL be able to remove any default entry.

#### Scenario: Defaults present on first run

- **WHEN** the app runs for the first time
- **THEN** the blacklist contains exactly the six default entries

---
### Requirement: Evaluation covers title and artist

The engine SHALL evaluate title and artist independently and SHALL report a match when either one matches any enabled rule, together with the reason that matched.

#### Scenario: Artist match triggers

- **WHEN** the title is "Song", the artist is "华语群星", and the Simplified Chinese rule is enabled
- **THEN** the engine reports a match with reason "simplified-artist"
