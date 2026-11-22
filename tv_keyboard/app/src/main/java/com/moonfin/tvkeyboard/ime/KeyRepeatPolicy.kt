package com.moonfin.tvkeyboard.ime

/** Pure policy for key repeat — no Android types. Testable on JVM. */
data class KeyRepeatPolicy(
    val initialDelayMs: Long = 400L,
    val intervalMs: Long = 50L,
) {
    fun isRepeatKey(keyCode: Int): Boolean = keyCode in REPEAT_KEY_CODES

    companion object {
        val REPEAT_KEY_CODES = setOf(KeyCodes.DELETE, KeyCodes.LEFT, KeyCodes.RIGHT)
        val DEFAULT = KeyRepeatPolicy()
    }
}
