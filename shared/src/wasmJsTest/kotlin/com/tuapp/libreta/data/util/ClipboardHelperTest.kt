package com.tuapp.libreta.data.util

import kotlin.test.Test
import kotlin.test.assertEquals

class ClipboardHelperTest {

    @Test
    fun `copyToClipboard with normal text does not throw`() {
        val text = "Hello, World!"
        ClipboardHelper.copyToClipboard(text)
    }

    @Test
    fun `copyToClipboard with empty string does not throw`() {
        val text = ""
        ClipboardHelper.copyToClipboard(text)
    }

    @Test
    fun `copyToClipboard with long text does not throw`() {
        val text = "A".repeat(10000)
        ClipboardHelper.copyToClipboard(text)
    }

    @Test
    fun `copyToClipboard with special characters does not throw`() {
        val text = "Hola! ¿Cómo estás? 测试 🎉"
        ClipboardHelper.copyToClipboard(text)
    }

    @Test
    fun `copyToClipboard with unicode characters does not throw`() {
        val text = "日本語テスト 한국어 العربية עברית"
        ClipboardHelper.copyToClipboard(text)
    }

    @Test
    fun `copyToClipboard with newlines does not throw`() {
        val text = "Line 1\nLine 2\nLine 3"
        ClipboardHelper.copyToClipboard(text)
    }

    @Test
    fun `copyToClipboard with tabs does not throw`() {
        val text = "Column1\tColumn2\tColumn3"
        ClipboardHelper.copyToClipboard(text)
    }

    @Test
    fun `copyToClipboard multiple times does not throw`() {
        ClipboardHelper.copyToClipboard("First")
        ClipboardHelper.copyToClipboard("Second")
        ClipboardHelper.copyToClipboard("Third")
    }

    @Test
    fun `copyToClipboard returns Unit`() {
        val result: Unit = ClipboardHelper.copyToClipboard("test")
        assertEquals(Unit, result)
    }
}