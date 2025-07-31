package com.bytedance.tools.codelocator.panels;

import com.bytedance.tools.codelocator.utils.ImageUtils;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;

public class ScaleImagePanel extends JPanel {

    private Image mImage;

    public int mImageWidth;

    public String fileName;

    public int mImageHeight;

    public int otherX;

    private Image mOptImage;

    ImageFilter filter = new RGBImageFilter() {
        int transparentColor = Color.white.getRGB() | 0xFF000000;

        public int filterRGB(int x, int y, int rgb) {
            if ((rgb | 0xFF000000) == transparentColor) {
                return 0x00FFFFFF & rgb;
            } else {
                return rgb;
            }
        }
    };

    public ScaleImagePanel(VirtualFile imageFile) {
        try {
            fileName = imageFile.getName();
            mImage = ImageUtils.getImage(imageFile.getInputStream());
            mImageWidth = mImage.getWidth(null);
            mImageHeight = mImage.getWidth(null);
            if (mImageWidth > 300) {
                mImageHeight = 300 * mImageHeight / mImageWidth;
                mImageWidth = 300;
            }
            ImageProducer filteredImgProd = new FilteredImageSource(mImage.getSource(), filter);
            mOptImage = Toolkit.getDefaultToolkit().createImage(filteredImgProd);
        } catch (Throwable t) {
        }
    }

    public void setOtherX(int x) {
        otherX = x;
    }

    public void adjustImageSize(int width, int height) {
        setPreferredSize(new Dimension(width, height));
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (isOpaque()) {
            paintChessboard(g);
            g.drawImage(mImage, 0, 0, getWidth(), getHeight(), null);
        } else {
            g.drawImage(mOptImage, 0, 0, getWidth(), getHeight(), null);
        }
    }

    @Override
    public void paintComponents(Graphics g) {
        super.paintComponents(g);
    }

    BufferedImage pattern;

    private Color whiteColor = Color.WHITE;

    private Color blackColor = Color.LIGHT_GRAY;

    @Override
    public void setOpaque(boolean isOpaque) {
        super.setOpaque(isOpaque);
        if (!isOpaque) {
            whiteColor = new Color(Color.WHITE.getRed(), Color.WHITE.getGreen(), Color.WHITE.getBlue(), 128);
            blackColor = new Color(Color.LIGHT_GRAY.getRed(), Color.LIGHT_GRAY.getGreen(), Color.LIGHT_GRAY.getBlue(), 128);
        } else {
            whiteColor = Color.WHITE;
            blackColor = Color.LIGHT_GRAY;
        }
    }

    private void paintChessboard(Graphics g) {
        Dimension size = new Dimension(getWidth(), getHeight());
        int cellSize = 5;
        int patternSize = 2 * cellSize;

        if (pattern == null) {
            pattern = UIUtil.createImage(this, patternSize, patternSize, BufferedImage.TYPE_INT_ARGB);
            Graphics imageGraphics = pattern.getGraphics();
            imageGraphics.setColor(whiteColor);
            imageGraphics.fillRect(0, 0, patternSize, patternSize);
            imageGraphics.setColor(blackColor);
            imageGraphics.fillRect(0, cellSize, cellSize, cellSize);
            imageGraphics.fillRect(cellSize, 0, cellSize, cellSize);
        }

        ((Graphics2D) g).setPaint(new TexturePaint(pattern, new Rectangle(0, 0, patternSize, patternSize)));
        g.fillRect(0, 0, size.width, size.height);
    }
}
