package com.bytedance.tools.codelocator.panels;

import javax.swing.*;
import java.awt.*;

public class GifPanel extends JPanel {

    private Image image;

    public GifPanel(Image gifImage) {
        image = gifImage;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, null);
    }

}
