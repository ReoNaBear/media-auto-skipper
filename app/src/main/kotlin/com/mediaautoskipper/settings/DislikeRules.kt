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
     * Carries flags across a rule list edit. When the size is unchanged, an entry whose text changed
     * is an edit: its flag moves to the new text. Otherwise entries that disappeared lose their flag.
     * Newly added entries start unflagged.
     */
    fun reconcile(flags: Set<String>, kind: RuleKind, old: List<String>, new: List<String>): Set<String> {
        val result = flags.toMutableSet()
        if (old.size == new.size) {
            old.indices.forEach { i ->
                if (old[i] != new[i] && result.remove(key(kind, old[i]))) result.add(key(kind, new[i]))
            }
        } else {
            old.filter { it !in new }.forEach { result.remove(key(kind, it)) }
        }
        return result
    }
}
