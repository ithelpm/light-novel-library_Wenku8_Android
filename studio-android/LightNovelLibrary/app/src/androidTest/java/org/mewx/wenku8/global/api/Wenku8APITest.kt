package org.mewx.wenku8.global.api

import org.junit.Test

class Wenku8APITest {
    @Test
    fun searchBadWordsTestValid() {
        val src = "就一些句子呗"
        assertNull(Wenku8API.searchBadWords(src))
    }

    @Test
    fun searchBadWordsTestInvalid() {
        val src = "就一些句子呗。 。 。 。"
        assertEquals("。。。。", Wenku8API.searchBadWords(src))
    }

    @Test
    fun searchBadWordsTestTraditionalInvalid() {
        val src = "就一些句子法輪功？？？"
        assertEquals("法輪功", Wenku8API.searchBadWords(src))
    }
}