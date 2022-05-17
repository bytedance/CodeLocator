package com.bytedance.tools.codelocator.listener

import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

open class DocumentListenerAdapter : DocumentListener {
    
    override fun changedUpdate(e: DocumentEvent?) {
    }

    override fun insertUpdate(e: DocumentEvent?) {
    }

    override fun removeUpdate(e: DocumentEvent?) {
    }
}