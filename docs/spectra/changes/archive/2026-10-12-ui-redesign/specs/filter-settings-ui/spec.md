## MODIFIED Requirements

### Requirement: Per-rule dislike option

Each keyword rule, each regular expression rule and the Simplified Chinese rule SHALL have its own option "also mark as dislike", off by default. The option SHALL be stored with the settings and survive an app restart. The Rules screen SHALL show a warning, in a visually distinct card, that marking a rule affects the user's account recommendations and cannot be undone by the app, and that it makes skipping slower and less reliable because the app waits before acting and some players move on by themselves or ignore the skip.

#### Scenario: Default is off

- **WHEN** the user adds a new keyword
- **THEN** its dislike option is off

#### Scenario: Option persists

- **WHEN** the user turns on the option for the keyword "Mix" and restarts the app
- **THEN** the option for "Mix" is still on

#### Scenario: Simplified Chinese rule

- **WHEN** the Simplified Chinese rule is on
- **THEN** its dislike option is available and can be turned on independently of keywords

#### Scenario: Simplified Chinese rule off

- **WHEN** the Simplified Chinese rule is turned off
- **THEN** its dislike option is disabled

#### Scenario: Warning about slower skipping

- **WHEN** the user opens the Rules screen
- **THEN** a warning is shown that names three consequences: the dislike is written to the account and affects recommendations, the app cannot undo it, and skipping becomes slower and less reliable

#### Scenario: Warning in every language

- **WHEN** the interface language is any of the five supported languages
- **THEN** the warning is shown in that language
