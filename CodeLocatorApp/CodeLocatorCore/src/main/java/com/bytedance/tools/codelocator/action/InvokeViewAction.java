package com.bytedance.tools.codelocator.action;

import android.util.Log;
import android.view.View;

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
public class InvokeViewAction extends ViewAction {

    @NonNull
    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.INVOKE;
    }

    @Override
    public void processViewAction(@NonNull View view, String data, @NonNull ResultData result) {
        InvokeInfo invokeInfo = GsonUtils.sGson.fromJson(data, InvokeInfo.class);
        String process = "";
        try {
            if (invokeInfo.getInvokeField() != null) {
                process = invokeSetField(view, invokeInfo.getInvokeField());
            } else if (invokeInfo.getInvokeMethod() != null) {
                process = invokeCallMethod(view, invokeInfo.getInvokeMethod());
            }
            result.addResultItem(CodeLocatorConstants.ResultKey.DATA, process);
        } catch (Throwable t) {
            result.addResultItem(CodeLocatorConstants.ResultKey.ERROR, CodeLocatorConstants.Error.ERROR_WITH_STACK_TRACE);
            result.addResultItem(CodeLocatorConstants.ResultKey.STACK_TRACE, Log.getStackTraceString(t));
        }
    }

    private String invokeSetField(View view, FieldInfo fieldInfo) {
        Class javaClass = view.getClass();
        Field field = null;
        String result = "false";
        while (javaClass != Object.class) {
            final Field[] declaredFields = javaClass.getDeclaredFields();
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
            switch (fieldInfo.getType()) {
                case "int":
                    field.set(view, Integer.parseInt(fieldInfo.getValue()));
                    break;
                case "boolean":
                    field.set(view, Boolean.parseBoolean(fieldInfo.getValue()));
                    break;
                case "byte":
                    field.set(view, Byte.parseByte(fieldInfo.getValue()));
                    break;
                case "float":
                    field.set(view, Float.parseFloat(fieldInfo.getValue()));
                    break;
                case "long":
                    field.set(view, Long.parseLong(fieldInfo.getValue()));
                    break;
                case "double":
                    field.set(view, Double.parseDouble(fieldInfo.getValue()));
                    break;
                case "short":
                    field.set(view, Short.parseShort(fieldInfo.getValue()));
                    break;
                case "char":
                    field.set(view, fieldInfo.getValue().toCharArray()[0]);
                    break;
                case "java.lang.String":
                    field.set(view, fieldInfo.getValue());
                    break;
                case "java.lang.CharSequence":
                    field.set(view, fieldInfo.getValue());
                    break;
                default:
                    throw new RuntimeException("Field类型不支持 " + fieldInfo.getName());
            }
            return "true";
        } catch (Throwable t) {
            result = Log.getStackTraceString(t);
            throw new RuntimeException("修改Field失败, 失败原因: " + result);
        }
    }

    private String invokeCallMethod(View view, MethodInfo methodInfo) {
        Class javaClass = view.getClass();
        Method method = null;
        String result = "";
        while (javaClass != Object.class) {
            final Method[] declaredMethods = javaClass.getDeclaredMethods();
            for (Method m : declaredMethods) {
                if (m.getName().equals(methodInfo.getName())
                        && ((methodInfo.getArgType() == null && m.getParameterTypes().length == 0)
                        || (m.getParameterTypes().length == 1 && methodInfo.getArgType().equals(m.getParameterTypes()[0].getName())))) {
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
            Object obj = null;
            final String argType = methodInfo.getArgType();
            if (argType == null) {
                obj = method.invoke(view);
            } else {
                switch (argType) {
                    case "int":
                        obj = method.invoke(view, Integer.parseInt(methodInfo.getArgValue()));
                        break;
                    case "boolean":
                        obj = method.invoke(view, Boolean.parseBoolean(methodInfo.getArgValue()));
                        break;
                    case "byte":
                        obj = method.invoke(view, Byte.parseByte(methodInfo.getArgValue()));
                        break;
                    case "float":
                        obj = method.invoke(view, Float.parseFloat(methodInfo.getArgValue()));
                        break;
                    case "long":
                        obj = method.invoke(view, Long.parseLong(methodInfo.getArgValue()));
                        break;
                    case "double":
                        obj = method.invoke(view, Double.parseDouble(methodInfo.getArgValue()));
                        break;
                    case "short":
                        obj = method.invoke(view, Short.parseShort(methodInfo.getArgValue()));
                        break;
                    case "char":
                        obj = method.invoke(view, methodInfo.getArgValue().toCharArray()[0]);
                        break;
                    case "java.lang.String":
                        obj = method.invoke(view, methodInfo.getArgValue());
                        break;
                    case "java.lang.CharSequence":
                        obj = method.invoke(view, methodInfo.getArgValue());
                        break;
                    default:
                        obj = method.invoke(view);
                        break;
                }
            }
            if ("void".equalsIgnoreCase(method.getReturnType().getName()) || method.getReturnType() == Void.class) {
                return "";
            } else {
                if (obj == null) {
                    return "null";
                }
                String formatJson = GsonUtils.formatJson(GsonUtils.sGson.toJson(obj));
                return "{\"type\": \"" + obj.getClass().getName() + "\", \"data\": " + formatJson.replace("\n", " ") + "}";
            }
        } catch (Throwable t) {
            result = Log.getStackTraceString(t);
            throw new RuntimeException("调用函数失败, 失败原因: " + result);
        }
    }
}
