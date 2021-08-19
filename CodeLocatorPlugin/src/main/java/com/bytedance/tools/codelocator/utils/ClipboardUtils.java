package com.bytedance.tools.codelocator.utils;

import com.bytedance.tools.codelocator.action.ImageTransferable;
import com.intellij.openapi.project.Project;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

public class ClipboardUtils {

    public static void copyContentToClipboard(Project project, String contentToCopy) {
        copyContentToClipboard(project, contentToCopy, true);
    }

    public static void copyContentToClipboard(Project project, String contentToCopy, boolean showNotification) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable trans = new StringSelection(contentToCopy);
        clipboard.setContents(trans, null);
        if (showNotification) {
            NotificationUtils.showNotification(project, "内容已拷贝至剪切板", 1500L);
        }
    }

    public static void copyImageToClipboard(Image image) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable transferable = new ImageTransferable(image);
        clipboard.setContents(transferable, null);
    }

    public static String readClipboardContent() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable transferable = clipboard.getContents(null);
        String result = "";
        if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                result = (String) transferable.getTransferData(DataFlavor.stringFlavor);
            } catch (Throwable t) {
                Log.e("获取剪切板内容失败", t);
            }
        }
        return result;
    }

}
