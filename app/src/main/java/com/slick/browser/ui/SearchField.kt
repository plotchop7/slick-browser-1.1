package com.slick.browser.ui

import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.viewinterop.AndroidView
import com.slick.browser.ui.theme.LocalSlickColors

@Composable
fun SlickSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    focused: Boolean,
    onFocused: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    incognito: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalSlickColors.current
    val keyboard = LocalSoftwareKeyboardController.current
    var editor by remember { mutableStateOf<EditText?>(null) }

    LaunchedEffect(focused) {
        if (focused) {
            editor?.requestFocus()
            editor?.setSelection(editor?.text?.length ?: 0)
            keyboard?.show()
        } else {
            editor?.clearFocus()
            keyboard?.hide()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            EditText(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setTextColor(colors.foreground.toArgb())
                setHintTextColor(colors.muted.toArgb())
                textSize = 15f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                gravity = Gravity.CENTER_VERTICAL
                isSingleLine = true
                setPadding(0, 0, 0, 0)
                inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI
                imeOptions = EditorInfo.IME_ACTION_GO or EditorInfo.IME_FLAG_NO_EXTRACT_UI
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                    override fun afterTextChanged(s: Editable?) {
                        val next = s?.toString().orEmpty()
                        if (next != value) onValueChange(next)
                    }
                })
                setOnFocusChangeListener { _, hasFocus -> onFocused(hasFocus) }
                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_GO) {
                        onSubmit()
                        true
                    } else false
                }
                editor = this
            }
        },
        update = { view ->
            if (view.text.toString() != value) {
                view.setText(value)
                view.setSelection(value.length)
            }
            view.setTextColor(colors.foreground.toArgb())
            view.imeOptions = EditorInfo.IME_ACTION_GO or EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                if (incognito) EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING else 0
            if (incognito) {
                view.setPrivateImeOptions("nm")
                view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            } else {
                view.setPrivateImeOptions(null)
                view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
            }
        }
    )
}
