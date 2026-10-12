package com.mediaautoskipper.settings

/**
 * Keys that mark a rule as "dislike and skip". They mirror the reason strings that
 * `TextFilterEngine` returns (`keyword:<text>`, `regex:<expr>`, `simplified-title`), with both
 * Simplified Chinese reasons sharing one key.
 */
object DislikeRules {
    const val SIMPLIFIED = "simplified"

    fun key(kind: RuleKind, text: String): String =
        if (kind == RuleKind.KEYWORD) "keyword:$text" else "regex:$text"

    /** The rule key behind a match [reason], or null for an unknown reason. */
    fun keyForReason(reason: String): String? = when {
        reason.startsWith("simplified-") -> SIMPLIFIED
        reason.startsWith("keyword:") || reason.startsWith("regex:") -> reason
        else -> null
    }

    /**
     * Carries flags across a rule list edit. When exactly one entry's text changed (same size), that is an
     * edit: its flag moves to the new text. Any other change (entries added, removed, reordered or several
     * edited at once) keeps the flags of entries that are still present and drops those of entries that
     * disappeared. Newly added entries start unflagged.
     */
    fun reconcile(flags: Set<String>, kind: RuleKind, old: List<String>, new: List<String>): Set<String> {
        val changed = if (old.size == new.size) old.indices.filter { old[it] != new[it] } else emptyList()
        if (changed.size == 1 && old.toSet().size == old.size && new.none { it == old[changed[0]] }) {
            val i = changed[0]
            val oldKey = key(kind, old[i])
            return if (oldKey in flags) flags - oldKey + key(kind, new[i]) else flags
        }
        return flags - old.filter { it !in new }.map { key(kind, it) }.toSet()
    }
}
