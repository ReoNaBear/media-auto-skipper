package com.mediaautoskipper

import com.mediaautoskipper.filter.FilterConfig
import com.mediaautoskipper.filter.TextFilterEngine
import kotlin.test.Test
import kotlin.test.assertTrue

class SmokeTest {
    @Test
    fun appModuleSeesFilterEngine() {
        assertTrue(TextFilterEngine.evaluate("爱你", "", FilterConfig()).matched)
    }
}
