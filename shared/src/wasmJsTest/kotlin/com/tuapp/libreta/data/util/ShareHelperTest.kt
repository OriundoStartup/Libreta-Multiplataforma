package com.tuapp.libreta.data.util

import kotlin.test.Test
import kotlin.test.assertEquals

class ShareHelperTest {

    @Test
    fun `shareText with normal text does not throw`() {
        val text = "Hello, World!"
        ShareHelper.shareText(text)
    }

    @Test
    fun `shareText with empty string does not throw`() {
        val text = ""
        ShareHelper.shareText(text)
    }

    @Test
    fun `shareText with long text does not throw`() {
        val text = "A".repeat(10000)
        ShareHelper.shareText(text)
    }

    @Test
    fun `shareText with special characters does not throw`() {
        val text = "Hola! ¿Cómo estás? 测试 🎉"
        ShareHelper.shareText(text)
    }

    @Test
    fun `shareText with unicode does not throw`() {
        val text = "日本語テスト 한국어 العربية עברית"
        ShareHelper.shareText(text)
    }

    @Test
    fun `shareText with multiline text does not throw`() {
        val text = "Title\n\nDescription of the content"
        ShareHelper.shareText(text)
    }

    @Test
    fun `shareText with URL-like text does not throw`() {
        val text = "Check this out: https://example.com/page?q=test"
        ShareHelper.shareText(text)
    }

    @Test
    fun `shareText returns Unit`() {
        val result: Unit = ShareHelper.shareText("test")
        assertEquals(Unit, result)
    }

    @Test
    fun `shareText called multiple times does not throw`() {
        ShareHelper.shareText("First share")
        ShareHelper.shareText("Second share")
        ShareHelper.shareText("Third share")
    }
}