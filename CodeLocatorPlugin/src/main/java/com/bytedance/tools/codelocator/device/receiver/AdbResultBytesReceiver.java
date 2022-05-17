package com.bytedance.tools.codelocator.device.receiver;

import com.android.ddmlib.IShellOutputReceiver;

import java.io.ByteArrayOutputStream;

public class AdbResultBytesReceiver implements IShellOutputReceiver {

    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    @Override
    public void addOutput(byte[] bytes, int off, int len) {
        stream.write(bytes, off, len);
    }

    @Override
    public void flush() {

    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    public byte[] getResult() {
        return stream.toByteArray();
    }
}
