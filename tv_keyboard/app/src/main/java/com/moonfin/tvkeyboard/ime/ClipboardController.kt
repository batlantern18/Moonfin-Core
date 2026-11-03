package com.moonfin.tvkeyboard.ime

import android.content.ClipData
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
    private val recentClips: MutableList<ClipItem> = mutableListOf()
    
    init {
        loadRecentClips()
    }
    
    /** Returns the list of recent text clips, most recent first. */
    fun getRecentClips(limit: Int = 10): List<ClipItem> {
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        if (!clipText.isNullOrBlank()) {
            if (recentClips.isEmpty() || recentClips.first().text != clipText) {
                recentClips.add(0, ClipItem(clipText))
                if (recentClips.size > limit) {
                    recentClips.removeAt(recentClips.size - 1)
                }
            }
        }
        return recentClips.toList()
    }
    
    /** Paste the given text at the cursor position with auto-space handling. */
    fun paste(text: String, inputConnection: InputConnection?, autoSpace: Boolean = false) {
        if (inputConnection == null) return
        
        inputConnection.commitText(text, 1)
        if (autoSpace) {
            inputConnection.commitText(" ", 1)
        }
    }
    
    /** Clear recent clips list. */
    fun clear() {
        recentClips.clear()
    }
    
    /** Check if clipboard has any text content. */
    fun hasText(): Boolean {
        val clip = clipboard.primaryClip
        if (clip == null || clip.itemCount == 0) return false
        return clip.getItemAt(0).text != null
    }
    
    private fun loadRecentClips() {
        // Track recent items as they're added via getRecentClips
    }
    
    data class ClipItem(val text: String)
}