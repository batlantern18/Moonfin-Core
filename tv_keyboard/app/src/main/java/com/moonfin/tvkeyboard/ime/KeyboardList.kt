package com.moonfin.tvkeyboard.ime

/** Formats the keyboards a device owner can manage, marking the active one. */
object KeyboardList {

    data class Entry(val id: String, val label: String, val active: Boolean)

    fun format(entries: List<Entry>, activeMarker: String, emptyText: String): String {
        if (entries.isEmpty()) return emptyText
        return entries.joinToString("\n") { entry ->
            if (entry.active) "• ${entry.label} — $activeMarker" else "• ${entry.label}"
        }
    }
}
