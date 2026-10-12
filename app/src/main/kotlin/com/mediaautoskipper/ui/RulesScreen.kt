package com.mediaautoskipper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mediaautoskipper.R
import com.mediaautoskipper.settings.DislikeRules
import com.mediaautoskipper.settings.EditResult
import com.mediaautoskipper.settings.RuleEdits
import com.mediaautoskipper.settings.RuleKind

object RuleTags {
    fun input(kind: RuleKind) = "rule_input_$kind"
    fun add(kind: RuleKind) = "rule_add_$kind"
    fun dislike(kind: RuleKind, item: String) = "rule_dislike_${kind}_$item"
}

/** One editable list of rules (keywords or regular expressions). */
@Composable
fun RuleSection(
    kind: RuleKind,
    items: List<String>,
    onChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    /** Rule keys (see `DislikeRules`) whose "also mark as dislike" option is on. */
    dislikeKeys: Set<String> = emptySet(),
    /** Called with the rule key and the new state; when null, the option is not shown. */
    onDislikeChange: ((String, Boolean) -> Unit)? = null,
) {
    var input by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<EditResult?>(null) }

    val title = if (kind == RuleKind.KEYWORD) R.string.rules_keywords_title else R.string.rules_regex_title
    val hint = if (kind == RuleKind.KEYWORD) R.string.rules_keyword_hint else R.string.rules_regex_hint

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)

        items.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(item, Modifier.weight(1f))
                TextButton(onClick = {
                    editing = index
                    input = item
                    error = null
                }) { Text(stringResource(R.string.action_edit)) }
                TextButton(onClick = {
                    onChange(RuleEdits.remove(items, index))
                    if (editing == index) {
                        editing = null
                        input = ""
                    }
                }) { Text(stringResource(R.string.action_delete)) }
            }
            if (onDislikeChange != null) {
                val key = DislikeRules.key(kind, item)
                DislikeOption(
                    checked = key in dislikeKeys,
                    onCheckedChange = { onDislikeChange(key, it) },
                    modifier = Modifier.testTag(RuleTags.dislike(kind, item)),
                )
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = {
                input = it
                error = null
            },
            label = { Text(stringResource(hint)) },
            singleLine = true,
            isError = error != null,
            modifier = Modifier.fillMaxWidth().testTag(RuleTags.input(kind)),
        )
        error?.let { Text(errorText(it), color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                val idx = editing
                val result = if (idx == null) RuleEdits.add(items, input, kind) else RuleEdits.replace(items, idx, input, kind)
                if (result is EditResult.Ok) {
                    onChange(result.list)
                    input = ""
                    editing = null
                    error = null
                } else {
                    error = result
                }
            },
            modifier = Modifier.padding(bottom = 8.dp).testTag(RuleTags.add(kind)),
        ) { Text(stringResource(if (editing == null) R.string.action_add else R.string.action_save)) }
    }
}

/** The "also mark as dislike" checkbox with its label. */
@Composable
fun DislikeOption(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled, modifier = modifier)
        Text(stringResource(R.string.dislike_option), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun errorText(result: EditResult): String = when (result) {
    EditResult.Blank -> stringResource(R.string.error_blank)
    EditResult.Duplicate -> stringResource(R.string.error_duplicate)
    is EditResult.InvalidRegex -> stringResource(R.string.error_invalid_regex, result.message)
    is EditResult.Ok -> ""
}
