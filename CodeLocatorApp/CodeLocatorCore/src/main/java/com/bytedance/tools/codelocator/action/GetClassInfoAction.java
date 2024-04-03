package com.bytedance.tools.codelocator.action;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.model.FieldInfo;
import com.bytedance.tools.codelocator.model.InvokeInfo;
import com.bytedance.tools.codelocator.model.MethodInfo;
import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;
import com.bytedance.tools.codelocator.utils.GsonUtils;
import com.bytedance.tools.codelocator.utils.ReflectUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by liujian.android on 2024/4/1
 *
 * @author liujian.android@bytedance.com
 */
public class GetClassInfoAction extends ApplicationAction {

    @NonNull
    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.GET_CLASS_INFO;
    }

    @Override
    public void processApplicationAction(@NonNull Application application, @NonNull Activity activity, @NonNull String data, @NonNull ResultData result) {
        InvokeInfo invokeInfo = GsonUtils.sGson.fromJson(data, InvokeInfo.class);
        String process = "";
        try {
            if (invokeInfo.getClassName() != null && invokeInfo.getInvokeField() != null) {
                process = invokeField(invokeInfo.getClassName(), invokeInfo.getInvokeField());
            } else if (invokeInfo.getClassName() != null && invokeInfo.getInvokeMethod() != null) {
                process = invokeMethod(invokeInfo.getClassName(), invokeInfo.getInvokeMethod());
            }
            result.addResultItem(CodeLocatorConstants.ResultKey.DATA, process);
        } catch (Throwable t) {
            result.addResultItem(
                    CodeLocatorConstants.ResultKey.ERROR,
                    CodeLocatorConstants.Error.ERROR_WITH_STACK_TRACE
            );
            result.addResultItem(
                    CodeLocatorConstants.ResultKey.STACK_TRACE,
                    Log.getStackTraceString(t)
            );
        }
    }

    private String invokeField(@NonNull String className, @NonNull FieldInfo fieldInfo) throws ClassNotFoundException {
        Class javaClass = Class.forName(className);
        Field field = null;
        String result = "false";
        while (javaClass != Object.class) {
            final Field[] declaredFields = javaClass.getDeclaredFields();
            if (declaredFields == null) {
                javaClass = javaClass.getSuperclass();
                continue;
            }
            for (Field f : declaredFields) {
                if (ReflectUtils.isLegalField(f) && f.getName().equals(fieldInfo.getName())) {
                    field = f;
                    break;
                }
            }
            if (field != null) {
                break;
            }
            javaClass = javaClass.getSuperclass();
        }
        if (field == null) {
            throw new RuntimeException("未找到对应的Field " + fieldInfo.getName());
        }
        try {
            field.setAccessible(true);
            Object obj = field.get(null);
            if (obj == null) {
                result = "null";
            } else if (obj instanceof String) {
                result = (String) obj;
            } else {
                result = GsonUtils.sGson.toJson(obj);
            }
            return result;
        } catch (Throwable t) {
            result = Log.getStackTraceString(t);
            throw new RuntimeException("获取Field失败, 失败原因: " + result);
        }
    }

    private String invokeMethod(String className, MethodInfo methodInfo) throws ClassNotFoundException {
        Class javaClass = Class.forName(className);
        Method method = null;
        String result = "";
        while (javaClass != Object.class) {
            final Method[] declaredMethods = javaClass.getDeclaredMethods();
            if (declaredMethods == null) {
                javaClass = javaClass.getSuperclass();
                continue;
            }
            for (Method m : declaredMethods) {
                if (methodInfo.getName().equals(m.getName())
                        && ((m.getParameterTypes().length == 1 && methodInfo.getArgType().equals(m.getParameterTypes()[0].getName()))
                        || (m.getParameterTypes().length == 0 && methodInfo.getArgType() == null))) {
                    method = m;
                    break;
                }
            }
            if (method != null) {
                break;
            }
            javaClass = javaClass.getSuperclass();
        }
        if (method == null) {
            throw new RuntimeException("未找到对应函数 " + methodInfo.getName());
        }
        try {
            method.setAccessible(true);
            Object obj = method.invoke(null);
            if ("void".equalsIgnoreCase(method.getReturnType().getName()) || method.getReturnType() == Void.class) {
                return "";
            } else {
                if (obj == null) {
                    result = "null";
                } else if (obj instanceof String) {
                    result = (String) obj;
                } else {
                    result = GsonUtils.sGson.toJson(obj);
                }
                return result;
            }
        } catch (Throwable t) {
            result = Log.getStackTraceString(t);
            throw new RuntimeException("调用函数失败, 失败原因: " + result);
        }
    }

}
