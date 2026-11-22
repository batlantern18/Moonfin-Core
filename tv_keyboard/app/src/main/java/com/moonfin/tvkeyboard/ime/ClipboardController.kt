package com.moonfin.tvkeyboard.ime

import android.content.ClipboardManager
import android.content.Context
import android.view.inputmethod.InputConnection

/**
 * Manages clipboard operations for the TV keyboard.
 * Shows recent clips that can be navigated and pasted via D-pad.
 */
class ClipboardController(
    private val context: Context,
    private val onPaste: (text: String) -> Unit,
    private val onDismiss: () -> Unit,
) {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val history: MutableList<ClipItem> = mutableListOf()

    companion object {
        const val MAX_CLIPS = 10
    }

    /** Snapshot: adds the current primary clip at top, deduplicates, caps. */
    fun snapshot(): List<ClipItem> {
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text
            ?.toString()?.trim()?.takeIf { it.isNotBlank() }
        if (clipText != null && (history.isEmpty() || history.first().text != clipText)) {
            history.removeAll { it.text == clipText }
            history.add(0, ClipItem(clipText))
            if (history.size > MAX_CLIPS) history.removeAt(history.size - 1)
        }
        return history.toList()
    }

    fun hasText(): Boolean {
        val clip = clipboard.primaryClip
        return clip != null && clip.itemCount > 0 && clip.getItemAt(0).text != null
    }

    fun clear() { history.clear() }

    fun getRecentClips(limit: Int = MAX_CLIPS): List<ClipItem> = snapshot().take(limit)

    fun paste(text: String, inputConnection: InputConnection?, autoSpace: Boolean = false) {
        if (inputConnection == null) return
        inputConnection.commitText(text, 1)
        if (autoSpace) {
            inputConnection.commitText(" ", 1)
        }
    }

    data class ClipItem(val text: String)
}
