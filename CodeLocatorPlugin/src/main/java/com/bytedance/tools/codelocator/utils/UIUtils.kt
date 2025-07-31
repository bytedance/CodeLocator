package com.bytedance.tools.codelocator.utils

import com.intellij.ui.DocumentAdapter
import javax.swing.event.DocumentEvent
import javax.swing.text.JTextComponent

fun JTextComponent.onTextChange(action: (DocumentEvent) -> Unit) {
    document.addDocumentListener(
        object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                action(e)
            }
        }
    )
}