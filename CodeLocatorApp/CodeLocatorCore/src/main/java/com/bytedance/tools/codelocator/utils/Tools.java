package com.bytedance.tools.codelocator.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;

import java.lang.reflect.Method;
import java.util.List;

public class Tools {

    public static void processTools(String command) throws Exception {
        if (CodeLocatorConstants.COMMAND_UPDATE_ACTIVITY.equals(command)) {
            notifyActivityServiceUpdate();
        }
    }

    private static void notifyActivityServiceUpdate() throws Exception {
        Method getServiceMethod = ReflectUtils.getClassMethod(Class.forName("android.os.ServiceManager"),
            "getService", new Class[]{String.class});
        IBinder obj = (IBinder) getServiceMethod.invoke(null, "activity");
        if (obj != null) {
            final Parcel obtain = Parcel.obtain();
            try {
                obj.transact(('_' << 24) | ('S' << 16) | ('P' << 8) | 'R', obtain, null, 0);
            } catch (Throwable ignore) {
            }
            obtain.recycle();
        }
    }

    public static boolean isMainProcess(Context context) {
        return context.getPackageName().equals(getProcessName(context));
    }

    public static String getProcessName(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo proInfo : runningApps) {
            if (proInfo.pid == android.os.Process.myPid()) {
                if (proInfo.processName != null) {
                    return proInfo.processName;
                }
            }
        }
        return null;
    }
}
