package com.forgis.ironmetrics.util

import java.util.Locale

object TimeUtils {

    /** Formatea segundos como HH:MM:SS. */
    fun format(totalSeconds: Int): String {
        val h = totalSeconds / 3600
        val m = totalSeconds % 3600 / 60
        val s = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    /** Admite "HH:MM:SS" o "MM:SS". Devuelve null si el formato no es válido. */
    fun parse(text: String): Int? {
        val parts = text.trim().split(":")
        if (parts.size !in 2..3 || parts.any { it.isEmpty() || !it.all(Char::isDigit) }) return null
        val nums = parts.map { it.toIntOrNull() ?: return null }
        val (h, m, s) = if (nums.size == 3) nums else listOf(0, nums[0], nums[1])
        if (m > 59 || s > 59) return null
        return h * 3600 + m * 60 + s
    }
}
