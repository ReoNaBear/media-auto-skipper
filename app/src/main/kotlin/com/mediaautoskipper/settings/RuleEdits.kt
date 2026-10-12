package com.mediaautoskipper.settings

import com.mediaautoskipper.filter.TextFilterEngine

enum class RuleKind { KEYWORD, REGEX }

sealed interface EditResult {
    data class Ok(val list: List<String>) : EditResult
    data object Blank : EditResult
    data object Duplicate : EditResult
    data class InvalidRegex(val message: String) : EditResult
}

/** Pure list edits for the keyword and regex rule lists; a rejected edit leaves the list untouched. */
object RuleEdits {

    fun add(list: List<String>, input: String, kind: RuleKind): EditResult =
        validate(list, input, kind, ignoreIndex = -1)?.let { it }
            ?: EditResult.Ok(list + input.trim())

    fun replace(list: List<String>, index: Int, input: String, kind: RuleKind): EditResult =
        validate(list, input, kind, ignoreIndex = index)
            ?: EditResult.Ok(list.toMutableList().also { it[index] = input.trim() })

    fun remove(list: List<String>, index: Int): List<String> =
        list.filterIndexed { i, _ -> i != index }

    private fun validate(list: List<String>, input: String, kind: RuleKind, ignoreIndex: Int): EditResult? {
        val value = input.trim()
        if (value.isEmpty()) return EditResult.Blank
        if (list.withIndex().any { (i, existing) -> i != ignoreIndex && existing == value }) return EditResult.Duplicate
        if (kind == RuleKind.REGEX) {
            TextFilterEngine.validateRegex(value)?.let { return EditResult.InvalidRegex(it) }
        }
        return null
    }
}
