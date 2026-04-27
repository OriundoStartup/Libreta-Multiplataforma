package com.tuapp.libreta.data.util

@JsFun("""function() { 
  return typeof crypto !== 'undefined' && crypto.randomUUID 
    ? crypto.randomUUID() 
    : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) { 
        var r = Math.random() * 16 | 0, 
            v = c === 'x' ? r : (r & 0x3 | 0x8); 
        return v.toString(16); 
      }); 
}""")
external fun cryptoRandomUUID(): String

private val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)

actual fun String?.isValidUUID(): Boolean = this != null && uuidRegex.matches(this)
actual fun randomUuidString(): String = cryptoRandomUUID()