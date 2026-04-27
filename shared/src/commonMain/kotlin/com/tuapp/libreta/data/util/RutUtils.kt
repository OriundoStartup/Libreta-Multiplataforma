package com.tuapp.libreta.data.util

object RutUtils {
    /**
     * Formatea un string como RUT chileno: 12.345.678-9
     */
    fun format(rut: String): String {
        val clean = rut.replace(Regex("[^0-9kK]"), "")
        if (clean.isEmpty()) return ""
        
        val body = if (clean.length > 1) clean.substring(0, clean.length - 1) else ""
        val dv = clean.substring(clean.length - 1).uppercase()
        
        if (body.isEmpty()) return dv
        
        val formattedBody = body.reversed().chunked(3).joinToString(".").reversed()
        return "$formattedBody-$dv"
    }

    /**
     * Valida si un string es un RUT chileno válido
     */
    fun isValid(rut: String): Boolean {
        val clean = rut.replace(Regex("[^0-9kK]"), "")
        if (clean.length < 8) return false
        
        val body = clean.substring(0, clean.length - 1)
        val dv = clean.substring(clean.length - 1).uppercase()
        
        var sum = 0
        var multiplier = 2
        for (i in body.length - 1 downTo 0) {
            sum += body[i].digitToInt() * multiplier
            multiplier = if (multiplier == 7) 2 else multiplier + 1
        }
        
        val expectedDv = 11 - (sum % 11)
        val dvChar = when (expectedDv) {
            11 -> "0"
            10 -> "K"
            else -> expectedDv.toString()
        }
        
        return dv == dvChar
    }
}
