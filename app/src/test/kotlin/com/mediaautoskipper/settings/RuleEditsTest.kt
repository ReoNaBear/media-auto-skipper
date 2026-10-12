package com.mediaautoskipper.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RuleEditsTest {
    private val base = listOf("Mix")

    @Test
    fun blankIsRejected() {
        assertEquals(EditResult.Blank, RuleEdits.add(base, "   ", RuleKind.KEYWORD))
        assertEquals(EditResult.Blank, RuleEdits.add(base, "", RuleKind.REGEX))
    }

    @Test
    fun keywordIsTrimmedAndAppended() {
        assertEquals(EditResult.Ok(listOf("Mix", "Nightcore")), RuleEdits.add(base, "  Nightcore ", RuleKind.KEYWORD))
    }

    @Test
    fun duplicateIsRejected() {
        assertEquals(EditResult.Duplicate, RuleEdits.add(base, "Mix", RuleKind.KEYWORD))
    }

    @Test
    fun invalidRegexIsRejectedAndValidOneAccepted() {
        assertIs<EditResult.InvalidRegex>(RuleEdits.add(emptyList(), "([", RuleKind.REGEX))
        assertEquals(EditResult.Ok(listOf("^\\[Live\\]")), RuleEdits.add(emptyList(), "^\\[Live\\]", RuleKind.REGEX))
    }

    @Test
    fun replaceKeepsOthersAndAllowsSameValue() {
        val list = listOf("a", "b")
        assertEquals(EditResult.Ok(listOf("a", "c")), RuleEdits.replace(list, 1, "c", RuleKind.KEYWORD))
        assertEquals(EditResult.Ok(list), RuleEdits.replace(list, 1, "b", RuleKind.KEYWORD))
        assertEquals(EditResult.Duplicate, RuleEdits.replace(list, 1, "a", RuleKind.KEYWORD))
    }

    @Test
    fun removeDropsOnlyThatEntry() {
        assertEquals(listOf("a", "c"), RuleEdits.remove(listOf("a", "b", "c"), 1))
    }
}
