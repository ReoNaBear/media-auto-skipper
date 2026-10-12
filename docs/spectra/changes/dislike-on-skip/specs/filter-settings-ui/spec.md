## ADDED Requirements

### Requirement: Per-rule dislike option

Each keyword rule, each regular expression rule and the Simplified Chinese rule SHALL have its own option "also mark as dislike", off by default. The option SHALL be stored with the settings and survive an app restart. The Rules screen SHALL show a note that marking a rule affects the user's account recommendations and cannot be undone by the app.

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

### Requirement: Dislike option follows rule edits

When the user edits the text of a rule, its dislike option SHALL stay as it was. When the user deletes a rule, its dislike option SHALL be removed, and a later rule with the same text SHALL start with the option off.

#### Scenario: Edit keeps the option

- **WHEN** the keyword "Mix" has the option on and the user edits it to "DJ Mix"
- **THEN** "DJ Mix" has the option on

#### Scenario: Delete clears the option

- **WHEN** the user deletes the keyword "Mix" that has the option on and later adds "Mix" again
- **THEN** the new "Mix" has the option off

### Requirement: Existing settings stay skip only

Settings saved before this feature existed SHALL load with every rule's dislike option off.

#### Scenario: Upgrade

- **WHEN** the stored settings have keywords and no dislike option data
- **THEN** every rule loads with the dislike option off
