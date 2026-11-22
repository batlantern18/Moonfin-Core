# Moonfin TV Keyboard

The Android TV input method in `tv_keyboard/`: a D-pad-driven keyboard that types into other apps' fields.

## Language

**Text editor**:
The module through which the keyboard changes the field the user is typing into — inserting text, deleting, moving the cursor, applying a suggestion, pasting, or performing the editor action.
_Avoid_: editor, gateway, input handler

**Edit transaction**:
One user intent applied to the field, together with the single render that follows it.
_Avoid_: refresh, sync, update, commit cycle

**Auto-space**:
The rule that committing a whole token — a suggestion or a paste — is followed by one space, while typing a character is not.
_Avoid_: trailing space, autospacing

**Field mode**:
What kind of field the user is typing into — text, email, password or numeric — decided from the field's input type.
_Avoid_: keyboard mode, input type, layout mode
