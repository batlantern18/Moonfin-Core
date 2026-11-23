package com.moonfin.tvkeyboard.debug

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Debug-only host application for exercising the keyboard against real text
 * fields: one editor per input type, plus the host-chosen window alignments.
 *
 * Launch with `am start -n com.moonfin.tvkeyboard/.debug.TestHostActivity
 * --es field <name>` to focus a field directly. Every change and editor action
 * is written to logcat under [TAG] so a test run can assert on the result.
 */
class TestHostActivity : Activity() {

    private lateinit var status: TextView
    private val fields = mutableMapOf<String, EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }

        content.addView(TextView(this).apply {
            text = "Moonfin IME Test Host"
            textSize = 20f
        })
        status = TextView(this).apply {
            text = "status: idle"
            textSize = 16f
            setPadding(0, 8, 0, 8)
        }
        content.addView(status)

        val navOnly = intent?.getBooleanExtra(EXTRA_NAV, false) == true
        if (navOnly) {
            addNavField(content)
            getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("moonfin", "moonfin clipboard test"))
            Log.i(TAG, "clipboard seeded")
        } else {
        addField(
            content, "text-search", "Text · actionSearch",
            InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_SEARCH,
        )
        addField(
            content, "text-next", "Text · actionNext",
            InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_NEXT,
        )
        addField(
            content, "text-done", "Text · actionDone",
            InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_DONE,
        )
        addField(
            content, "text-go", "Text · actionGo",
            InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_GO,
        )
        addField(
            content, "email", "Email · actionDone",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            EditorInfo.IME_ACTION_DONE,
        )
        addField(
            content, "url", "URL · address cues",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
            EditorInfo.IME_ACTION_DONE,
        )
        addField(
            content, "password", "Password · actionDone",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            EditorInfo.IME_ACTION_DONE,
        )
        addField(
            content, "number", "Number · actionDone",
            InputType.TYPE_CLASS_NUMBER, EditorInfo.IME_ACTION_DONE,
        )
        addField(
            content, "multiline", "Multiline notes · actionDone",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            EditorInfo.IME_ACTION_DONE, maxLines = 4,
        )
        addField(
            content, "align-right", "Text · horizontalAlignment=right",
            InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_DONE,
            privateImeOptions = "horizontalAlignment=right",
        )
        addField(
            content, "align-left", "Text · horizontalAlignment=left",
            InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_DONE,
            privateImeOptions = "horizontalAlignment=left",
        )
        }

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(content, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ))
        })

        val requested = intent?.getStringExtra(EXTRA_FIELD)
        val target = fields[requested] ?: fields["text-search"]
        target?.post {
            target.requestFocus()
            getSystemService(Context.INPUT_METHOD_SERVICE)
                .let { it as InputMethodManager }
                .showSoftInput(target, InputMethodManager.SHOW_IMPLICIT)
            Log.i(TAG, "focus[$requested]")
        }
    }

    /**
     * A single field that leaves navigation keys for the IME, the way a TV app
     * does: the remote then drives the keyboard instead of the host screen.
     */
    private fun addNavField(parent: LinearLayout) {
        parent.addView(TextView(this).apply {
            text = "TV field · the D-pad drives the keyboard"
            textSize = 14f
            setPadding(0, 12, 0, 2)
        })
        val field = NavField(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            hint = "nav"
            isFocusableInTouchMode = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setOnEditorActionListener { _, actionId, event ->
                Log.i(TAG, "action[nav]=$actionId event=${event?.keyCode ?: 0}")
                status.text = "status: action[nav]=$actionId"
                true
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    Log.i(TAG, "text[nav]=$s")
                    status.text = "status: text[nav]=$s"
                }
            })
        }
        fields["nav"] = field
        parent.addView(field)
    }

    /** Leaves D-pad and select to the IME, as a remote-first TV screen does. */
    private class NavField @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : EditText(context, attrs) {

        override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
            if (keyCode in KEYBOARD_KEYS) false else super.onKeyDown(keyCode, event)

        override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
            if (keyCode in KEYBOARD_KEYS) false else super.onKeyUp(keyCode, event)

        private companion object {
            val KEYBOARD_KEYS = setOf(
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
            )
        }
    }

    private fun addField(
        parent: LinearLayout,
        name: String,
        label: String,
        inputType: Int,
        imeOptions: Int,
        maxLines: Int = 1,
        privateImeOptions: String? = null,
    ) {
        parent.addView(TextView(this).apply {
            text = label
            textSize = 14f
            setPadding(0, 12, 0, 2)
        })
        val field = EditText(this).apply {
            this.inputType = inputType
            this.imeOptions = imeOptions
            this.maxLines = maxLines
            isSingleLine = maxLines == 1
            hint = name
            isFocusableInTouchMode = true
            privateImeOptions?.let { setPrivateImeOptions(it) }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setOnEditorActionListener { _, actionId, event ->
                Log.i(TAG, "action[$name]=$actionId event=${event?.keyCode ?: 0}")
                status.text = "status: action[$name]=$actionId"
                true
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    Log.i(TAG, "text[$name]=$s")
                    status.text = "status: text[$name]=$s"
                }
            })
        }
        fields[name] = field
        parent.addView(field)
    }

    companion object {
        const val TAG = "TestHost"
        const val EXTRA_FIELD = "field"
        const val EXTRA_NAV = "nav"
    }
}
