package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.AutoUpdateUtils;
import com.bytedance.tools.codelocator.utils.DataUtils;
import com.bytedance.tools.codelocator.utils.FileUtils;
import com.bytedance.tools.codelocator.utils.Log;
import com.bytedance.tools.codelocator.utils.GsonUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class CodeLocatorInfo {

    private static final String CODELOCATOR_TAG = "CodeLocator";

    private static final String CODELOCATOR_VERSION = AutoUpdateUtils.getCurrentPluginVersion();

    public static final int INT_SIZE = 4;

    private WApplication mWApplication;

    private Image mImage;

    public CodeLocatorInfo(WApplication application, Image image) {
        mWApplication = application;
        this.mImage = image;
    }

    public WApplication getWApplication() {
        return mWApplication;
    }

    public void setWApplication(WApplication mWApplication) {
        this.mWApplication = mWApplication;
    }

    public Image getImage() {
        return mImage;
    }

    public void setImage(Image mImage) {
        this.mImage = mImage;
    }

    public byte[] toBytes() {
        try {
            final String appStr = GsonUtils.sGson.toJson(mWApplication);
            final byte[] appBytes = appStr.getBytes(FileUtils.CHARSET_NAME);
            final byte[] codelocatorBytes = CODELOCATOR_TAG.getBytes(FileUtils.CHARSET_NAME);
            final byte[] codelocatorVersionBytes = CODELOCATOR_VERSION.getBytes(FileUtils.CHARSET_NAME);
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(appBytes.length);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

            dataOutputStream.writeInt(codelocatorBytes.length);
            dataOutputStream.write(codelocatorBytes);
            dataOutputStream.writeInt(codelocatorVersionBytes.length);
            dataOutputStream.write(codelocatorVersionBytes);
            dataOutputStream.writeInt(appBytes.length);
            dataOutputStream.write(appBytes);
            if (mImage instanceof BufferedImage) {
                ImageIO.write((BufferedImage) mImage, "PNG", dataOutputStream);
            } else {
                BufferedImage bufferedImage = new BufferedImage(mImage.getWidth(null),
                        mImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                bufferedImage.getGraphics().drawImage(mImage, 0, 0, null);
                ImageIO.write(bufferedImage, "PNG", dataOutputStream);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (Throwable t) {
            Log.e("二进制化CodeLocator数据失败", t);
        }
        return null;
    }

    public static CodeLocatorInfo fromCodeLocatorInfo(byte[] bytes) {
        try {
            DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(bytes));

            final int codelocatorLength = inputStream.readInt();
            int offset = INT_SIZE;

            byte[] codelocatorBytes = new byte[codelocatorLength];
            inputStream.readFully(codelocatorBytes);
            offset += codelocatorLength;

            final String s = new String(codelocatorBytes, FileUtils.CHARSET_NAME);
            if (!CODELOCATOR_TAG.equals(s)) {
                return null;
            }

            final int codelocatorVersionLength = inputStream.readInt();
            offset += INT_SIZE;
            byte[] codelocatorVersionBytes = new byte[codelocatorVersionLength];
            inputStream.readFully(codelocatorVersionBytes);
            offset += codelocatorVersionLength;
            final String version = new String(codelocatorVersionBytes, FileUtils.CHARSET_NAME);

            final int appLength = inputStream.readInt();
            offset += INT_SIZE;

            byte[] appBytes = new byte[appLength];
            inputStream.readFully(appBytes);
            offset += appLength;

            final String appStr = new String(appBytes, FileUtils.CHARSET_NAME);
            WApplication application = GsonUtils.sGson.fromJson(appStr, WApplication.class);
            DataUtils.restoreAllStructInfo(application, true);

            final int imageLength = bytes.length - offset;
            byte[] imageBytes = new byte[imageLength];
            inputStream.readFully(imageBytes);
            final BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (application == null || image == null) {
                return null;
            }
            return new CodeLocatorInfo(application, image);
        } catch (Throwable t) {
            Log.e("恢复CodeLocator数据失败", t);
        }
        return null;
    }
}
