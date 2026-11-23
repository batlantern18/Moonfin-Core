package com.moonfin.tvkeyboard.ime

/** What the strip above the keys is showing. */
enum class TopBarKind { EMPTY, SUGGESTIONS, DOMAINS, DIGITS, VOICE, CUES }

/**
 * The strip's contents, decided once from the field mode, the voice state and
 * the suggestions the engine produced. The view renders this; it never works
 * out for itself which of the four it should be showing.
 *
 * [Suggestion.replacePrefix] carries the item's meaning: a positive prefix marks
 * a word completion that replaces what was typed, while zero marks a whole token
 * (a domain, a digit) that is inserted as-is.
 */
data class TopBarContent(
    val kind: TopBarKind,
    val items: List<Suggestion> = emptyList(),
    val voiceState: VoiceState = VoiceState.IDLE,
    val voiceError: String = "",
) {
    /** Voice always shows; otherwise the strip needs at least one item. */
    val visible: Boolean
        get() = when (kind) {
            TopBarKind.EMPTY -> false
            TopBarKind.VOICE -> true
            else -> items.isNotEmpty()
        }

    companion object {
        val EMPTY = TopBarContent(TopBarKind.EMPTY)

        /** Address pieces that finish what most hosts actually ask for. */
        val URL_CUES: List<Suggestion> =
            listOf("https://", "http://", "www.", "jellyfin", ".com", ".org", ".net", ":8096", "/")
                .map { Suggestion(it, 0) }

        /** Digits and the symbols a password or code actually contains. */
        val DIGIT_CHIPS: List<String> =
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "@", ".", "_", "-", "!", "#")

        fun of(
            fieldMode: FieldMode,
            voiceState: VoiceState = VoiceState.IDLE,
            voiceError: String = "",
            suggestions: List<Suggestion> = emptyList(),
            suggestionsEnabled: Boolean = true,
        ): TopBarContent {
            if (voiceState != VoiceState.IDLE) {
                return TopBarContent(
                    kind = TopBarKind.VOICE,
                    voiceState = voiceState,
                    voiceError = voiceError,
                )
            }
            return when {
                fieldMode == FieldMode.PASSWORD ->
                    TopBarContent(TopBarKind.DIGITS, DIGIT_CHIPS.map { Suggestion(it, 0) })

                fieldMode == FieldMode.URL ->
                    TopBarContent(TopBarKind.CUES, URL_CUES)

                fieldMode == FieldMode.EMAIL && suggestions.isNotEmpty() ->
                    TopBarContent(TopBarKind.DOMAINS, suggestions)

                suggestionsEnabled && suggestions.isNotEmpty() ->
                    TopBarContent(TopBarKind.SUGGESTIONS, suggestions)

                else -> EMPTY
            }
        }
    }
}
