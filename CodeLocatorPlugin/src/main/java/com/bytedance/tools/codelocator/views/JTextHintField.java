package com.bytedance.tools.codelocator.views;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.*;

public class JTextHintField extends JTextField implements FocusListener {

    private String mHintStr;

    private Color mOriginColor;
    private Color mHintColor;

    public JTextHintField(String text) {
        super(text);
        addFocusListener(this);
        mOriginColor = getForeground();
        mHintColor = getDisabledTextColor();
    }

    public String getCurrentText() {
        return super.getText();
    }

    @Override
    public void setText(String t) {
        super.setText(t);
        if (t != null && !t.isEmpty() && t != mHintStr) {
            setForeground(mOriginColor);
        }
    }

    @Override
    public String getText() {
        String text = super.getText();
        if (text != null && text.equals(mHintStr) && getForeground().equals(mHintColor)) {
            return "";
        }
        return text;
    }

    public void setHint(String hint) {
        if (getCurrentText().equals(mHintStr)) {
            setText("");
        }
        mHintStr = hint;
        focusLost(null);
        repaint();
    }

    @Override
    public void focusGained(FocusEvent e) {
        String currentText = getCurrentText();
        if (isEditable() && currentText.equals(mHintStr) && getForeground().equals(mHintColor)) {
            setText("");
            setForeground(mOriginColor);
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        String currentText = getCurrentText();
        if (isEditable() && "".equals(currentText)) {
            setForeground(mHintColor);
            setText(mHintStr);
        }
    }
}
