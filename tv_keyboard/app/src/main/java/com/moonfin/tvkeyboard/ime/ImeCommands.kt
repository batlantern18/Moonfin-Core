package com.moonfin.tvkeyboard.ime

import java.util.Locale

/**
 * What the IME service should do for one key press. Keeping the decision in a
 * pure type lets the text pipeline be tested on the JVM without an editor.
 */
sealed class ImeCommand {
    /** A word character: extend the composition so predictions can follow it. */
    data class ComposeText(val text: String) : ImeCommand()
    data class InsertText(val text: String) : ImeCommand()
    data object DeleteBeforeCursor : ImeCommand()
    data object MoveLeft : ImeCommand()
    data object MoveRight : ImeCommand()
    data object ToggleShift : ImeCommand()
    data object LockShift : ImeCommand()
    data object ToggleMode : ImeCommand()
    data object CycleLanguage : ImeCommand()
    data object VoiceInput : ImeCommand()
    data object ClipboardOverlay : ImeCommand()
    data object None : ImeCommand()
}

/** Maps a keyboard key to the editor operation it performs. */
object ImeCommandResolver {

    fun resolve(
        keyCode: Int,
        label: CharSequence?,
        shifted: Boolean,
        locale: Locale,
    ): ImeCommand = when (TextCommitter.resolve(keyCode)) {
        KeyAction.TEXT ->
            TextCommitter.textFor(keyCode, label, shifted, locale)
                ?.let { text ->
                    if (TextCommitter.composes(text)) {
                        ImeCommand.ComposeText(text)
                    } else {
                        ImeCommand.InsertText(text)
                    }
                }
                ?: ImeCommand.None
        KeyAction.SPACE -> ImeCommand.InsertText(" ")
        KeyAction.DELETE -> ImeCommand.DeleteBeforeCursor
        KeyAction.CURSOR_LEFT -> ImeCommand.MoveLeft
        KeyAction.CURSOR_RIGHT -> ImeCommand.MoveRight
        KeyAction.SHIFT -> ImeCommand.ToggleShift
        KeyAction.MODE_CHANGE -> ImeCommand.ToggleMode
        KeyAction.LANGUAGE -> ImeCommand.CycleLanguage
        KeyAction.VOICE -> ImeCommand.VoiceInput
        KeyAction.CLIPBOARD -> ImeCommand.ClipboardOverlay
        KeyAction.NONE,
        -> ImeCommand.None
    }
}
