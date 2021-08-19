package com.bytedance.tools.codelocator.utils;

import java.awt.*;

import javax.swing.*;

public class JComponentUtils {

    public static void setSize(JComponent jComponent, int width, int height) {
        jComponent.setMinimumSize(new Dimension(width, height));
        jComponent.setMaximumSize(new Dimension(width, height));
        jComponent.setPreferredSize(new Dimension(width, height));
        jComponent.setSize(new Dimension(width, height));
    }

    public static void setMinimumSize(JComponent jComponent, int width, int height) {
        jComponent.setMinimumSize(new Dimension(width, height));
        jComponent.setPreferredSize(new Dimension(width, height));
        jComponent.setMaximumSize(new Dimension(10086, height));
    }

}
