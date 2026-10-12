package com.mediaautoskipper.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.mediaautoskipper.R

/** Interface language options. [tag] is empty for "Follow system". [nativeName] is shown in its own language. */
enum class LanguageChoice(val tag: String, val nativeName: String?) {
    SYSTEM("", null),
    ENGLISH("en", "English"),
    KOREAN("ko", "한국어"),
    JAPANESE("ja", "日本語"),
    SIMPLIFIED_CHINESE("zh-CN", "简体中文"),
    TRADITIONAL_CHINESE("zh-TW", "繁體中文"),
    ;

    companion object {
        fun fromTag(tag: String): LanguageChoice =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: SYSTEM
    }
}

object LanguageTags {
    fun option(choice: LanguageChoice) = "language_${choice.name}"
}

/** Applies the choice through AppCompat per-app locales; it persists and recreates the activity. */
fun applyLanguage(choice: LanguageChoice) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(choice.tag))
}

@Composable
fun LanguageSection(modifier: Modifier = Modifier) {
    var current by remember {
        mutableStateOf(LanguageChoice.fromTag(AppCompatDelegate.getApplicationLocales().toLanguageTags()))
    }
    LanguageOptions(current, modifier) {
        current = it
        applyLanguage(it)
    }
}

@Composable
fun LanguageOptions(current: LanguageChoice, modifier: Modifier = Modifier, onSelect: (LanguageChoice) -> Unit) {
    SectionCard(stringResource(R.string.settings_language), modifier) {
        LanguageChoice.entries.forEach { choice ->
            Row(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = Dimens.touchTarget).testTag(LanguageTags.option(choice))
                    .selectable(selected = choice == current, role = Role.RadioButton, onClick = { onSelect(choice) }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = choice == current, onClick = null)
                Text(
                    choice.nativeName ?: stringResource(R.string.language_follow_system),
                    Modifier.padding(start = Dimens.sm),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
