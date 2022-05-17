package com.bytedance.tools.codelocator.device.receiver;

import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.RawImage;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.*;
import java.io.IOException;

public class AdbResultImageReceiver implements IShellOutputReceiver {

    private RawImage mRawImage;

    private BufferedImage mImage;

    public void setData(RawImage rawImage) {
        mRawImage = rawImage;
        mImage = createImage(mRawImage);
    }

    public Image getResult() {
        return mImage;
    }

    @Nullable
    private static String getProfileName(RawImage image) {
        switch (image.colorSpace) {
            case RawImage.COLOR_SPACE_UNKNOWN:
                return null;
            case RawImage.COLOR_SPACE_SRGB:
                return "sRGB.icc";
            case RawImage.COLOR_SPACE_DISPLAY_P3:
                return "DisplayP3.icc";
        }
        return null;
    }

    private BufferedImage createImage(RawImage rawImage) {
        BufferedImage myImage = createBufferedImage(rawImage);
        final int colorBits = rawImage.bpp / 8;
        for (int y = 0; y < rawImage.height; y++) {
            for (int x = 0; x < rawImage.width; x++) {
                int argb = rawImage.getARGB((x + y * rawImage.width) * colorBits);
                myImage.setRGB(x, y, argb);
            }
        }
        return myImage;
    }

    private BufferedImage createBufferedImage(RawImage rawImage) {
        String profileName = getProfileName(rawImage);
        if (profileName == null) {
            return new BufferedImage(rawImage.width, rawImage.height, BufferedImage.TYPE_INT_ARGB);
        }
        ICC_Profile profile = ICC_Profile.getInstance(ColorSpace.CS_sRGB);
        try {
            profile = ICC_Profile.getInstance(getClass().getClassLoader().getResourceAsStream("colorProfiles/" + profileName));
        } catch (IOException e) {
        }
        ICC_ColorSpace colorSpace = new ICC_ColorSpace(profile);

        ColorModel colorModel = new DirectColorModel(colorSpace, 32, 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000, false, DataBuffer.TYPE_INT);
        WritableRaster raster = colorModel.createCompatibleWritableRaster(rawImage.width, rawImage.height);
        return new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null);
    }

    @Override
    public void addOutput(byte[] bytes, int i, int i1) {

    }

    @Override
    public void flush() {

    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}
