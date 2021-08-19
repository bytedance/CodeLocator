package com.bytedance.tools.codelocator.action;

import com.bytedance.tools.codelocator.listener.OnActionListener;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SimpleAction extends AnAction {

    private OnActionListener mOnActionListener;

    public SimpleAction(String text, OnActionListener onActionListener) {
        this(text, null, onActionListener);
    }

    public SimpleAction(String text, Icon icon, OnActionListener onActionListener) {
        super(text, text, icon);
        mOnActionListener = onActionListener;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (mOnActionListener != null) {
            mOnActionListener.actionPerformed(e);
        }
    }
}
