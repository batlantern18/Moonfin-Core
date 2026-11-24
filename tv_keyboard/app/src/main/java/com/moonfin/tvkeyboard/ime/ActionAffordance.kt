package com.moonfin.tvkeyboard.ime

import java.util.Locale
import android.view.inputmethod.EditorInfo

/**
 * Pure mapping from an editor action ID to the affordance that should be
 * displayed on the primary action key. This is derived from the editor action
 * (imeOptions), not from Field mode, so it can be tested on the JVM without
 * any Android view dependencies.
 */
object ActionAffordance {

    /** The label / icon to show on the action button for a given editor action. */
    data class Affordance(
        /** The content description / label for accessibility. */
        val contentDescription: String,
        /** The short label shown on the button (e.g. "Search", "Go", "Done"). */
        val label: String,
        /** A glyph/character to render in the action button, or null for text-only. */
        val glyph: String?,
    )

    /** Map an editor action ID to its affordance. */
    fun affordanceFor(action: Int): Affordance = when (action) {
        EditorInfo.IME_ACTION_SEARCH -> Affordance(
            contentDescription = "Search",
            label = "Search",
            glyph = "🔍",
        )
        EditorInfo.IME_ACTION_NEXT -> Affordance(
            contentDescription = "Next",
            label = "Next",
            glyph = "→",
        )
        EditorInfo.IME_ACTION_DONE -> Affordance(
            contentDescription = "Done",
            label = "Done",
            glyph = "✓",
        )
        EditorInfo.IME_ACTION_GO -> Affordance(
            contentDescription = "Go",
            label = "Go",
            glyph = "Go",
        )
        else -> Affordance(
            contentDescription = "Enter",
            label = "Enter",
            glyph = null,
        )
    }
}