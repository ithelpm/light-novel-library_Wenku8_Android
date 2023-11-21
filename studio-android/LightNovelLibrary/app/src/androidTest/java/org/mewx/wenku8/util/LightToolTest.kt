package org.mewx.wenku8.util

import androidx.test.filters.SmallTest

@SmallTest
class LightToolTest {
    @Test
    fun isInteger() {
        // true cases
        assertTrue(LightTool.isInteger("0"))
        assertTrue(LightTool.isInteger("1"))

        // false cases
        assertFalse(LightTool.isInteger(""))
        assertFalse(LightTool.isInteger("1.0"))
        assertFalse(LightTool.isInteger("1.."))
        assertFalse(LightTool.isInteger("a"))
    }

    @Test
    fun isDouble() {
        // true
        assertTrue(LightTool.isDouble("0.0"))
        assertTrue(LightTool.isDouble("1.0"))
        assertTrue(LightTool.isDouble("-1.0000009"))

        // false
        assertFalse(LightTool.isDouble(""))
        assertFalse(LightTool.isDouble("0"))
        assertFalse(LightTool.isDouble("1"))
        assertFalse(LightTool.isDouble("-1..0000009"))
        assertFalse(LightTool.isDouble("a"))
    }
}