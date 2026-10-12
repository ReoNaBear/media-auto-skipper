package com.mediaautoskipper.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DislikeRulesTest {

    @Test
    fun matchReasonsMapToRuleKeys() {
        assertEquals(DislikeRules.SIMPLIFIED, DislikeRules.keyForReason("simplified-title"))
        assertEquals(DislikeRules.SIMPLIFIED, DislikeRules.keyForReason("simplified-artist"))
        assertEquals("keyword:合輯", DislikeRules.keyForReason("keyword:合輯"))
        assertEquals("""regex:^\[Live\]""", DislikeRules.keyForReason("""regex:^\[Live\]"""))
        assertNull(DislikeRules.keyForReason("something-else"))
    }

    @Test
    fun editMovesTheFlagToTheNewText() {
        val flags = setOf("keyword:Mix", "regex:Mix")
        val result = DislikeRules.reconcile(flags, RuleKind.KEYWORD, listOf("Mix", "DJ"), listOf("DJ Mix", "DJ"))
        assertEquals(setOf("keyword:DJ Mix", "regex:Mix"), result)
    }

    @Test
    fun deleteDropsTheFlagAndReAddingStartsUnflagged() {
        val afterDelete = DislikeRules.reconcile(setOf("keyword:Mix"), RuleKind.KEYWORD, listOf("Mix", "DJ"), listOf("DJ"))
        assertEquals(emptySet(), afterDelete)
        val afterReAdd = DislikeRules.reconcile(afterDelete, RuleKind.KEYWORD, listOf("DJ"), listOf("DJ", "Mix"))
        assertEquals(emptySet(), afterReAdd)
    }

    @Test
    fun addingAnEntryKeepsExistingFlags() {
        val result = DislikeRules.reconcile(setOf("keyword:Mix"), RuleKind.KEYWORD, listOf("Mix"), listOf("Mix", "DJ"))
        assertEquals(setOf("keyword:Mix"), result)
    }

    @Test
    fun reorderingKeepsEveryFlag() {
        val flags = setOf("keyword:A", "keyword:B")
        val result = DislikeRules.reconcile(flags, RuleKind.KEYWORD, listOf("A", "B", "C"), listOf("B", "C", "A"))
        assertEquals(flags, result)
    }

    @Test
    fun severalEditsAtOnceKeepFlagsOfEntriesThatStayAndDropTheRest() {
        val flags = setOf("keyword:A", "keyword:B")
        val result = DislikeRules.reconcile(flags, RuleKind.KEYWORD, listOf("A", "B", "C"), listOf("A", "X", "Y"))
        assertEquals(setOf("keyword:A"), result)
    }
}
