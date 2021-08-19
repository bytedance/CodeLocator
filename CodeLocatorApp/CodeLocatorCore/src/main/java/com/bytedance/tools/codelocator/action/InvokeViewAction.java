package com.bytedance.tools.codelocator.action;

import android.util.Log;
import android.view.View;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.model.FieldInfo;
import com.bytedance.tools.codelocator.model.InvokeInfo;
import com.bytedance.tools.codelocator.model.MethodInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class InvokeViewAction extends ViewAction {
    @Override
    public String getActionType() {
        return "IK";
    }

    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
        InvokeInfo invokeInfo = CodeLocator.sGson.fromJson(actionContent, InvokeInfo.class);
        String process = "未知错误, 请点击小飞机反馈";
        if (invokeInfo.getInvokeField() != null) {
            process = invokeSetField(view, invokeInfo.getInvokeField());
        } else if (invokeInfo.getInvokeMethod() != null) {
            process = invokeCallMethod(view, invokeInfo.getInvokeMethod());
        }
        resultSb.append(process);
    }

    private String invokeSetField(View view, FieldInfo fieldInfo) {
        Class javaClass = view.getClass();
        Field field = null;
        String result = "false";
        while (javaClass != Object.class) {
            final Field[] declaredFields = javaClass.getDeclaredFields();
            for (Field f : declaredFields) {
                if (f.getName().equals(fieldInfo.getName()) && GetAllViewClassInfoAction.isLegalField(f)) {
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
            return "未找到对应的Field " + fieldInfo.getName();
        }
        try {
            field.setAccessible(true);
            switch (fieldInfo.getType()) {
                case "int":
                    field.set(view, Integer.valueOf(fieldInfo.getValue()));
                    break;
                case "boolean":
                    field.set(view, Boolean.valueOf(fieldInfo.getValue()));
                    break;
                case "byte":
                    field.set(view, Byte.valueOf(fieldInfo.getValue()));
                    break;
                case "float":
                    field.set(view, Float.valueOf(fieldInfo.getValue()));
                    break;
                case "long":
                    field.set(view, Long.valueOf(fieldInfo.getValue()));
                    break;
                case "double":
                    field.set(view, Double.valueOf(fieldInfo.getValue()));
                    break;
                case "short":
                    field.set(view, Short.valueOf(fieldInfo.getValue()));
                    break;
                case "char":
                    field.set(view, fieldInfo.getValue().toCharArray()[0]);
                    break;
                case "java.lang.String":
                case "java.lang.CharSequence":
                    field.set(view, fieldInfo.getValue());
                    break;
                default:
                    return "Field类型不支持";
            }
            return "true";
        } catch (Throwable t) {
            result = Log.getStackTraceString(t);
        }
        return "修改Field失败, 失败原因: " + result;
    }

    private String invokeCallMethod(View view, MethodInfo methodInfo) {
        Class javaClass = view.getClass();
        Method method = null;
        String result = "";
        while (javaClass != Object.class) {
            final Method[] declaredMethods = javaClass.getDeclaredMethods();
            for (Method m : declaredMethods) {
                if (m.getName().equals(methodInfo.getName())
                        && ((m.getParameterTypes().length == 0 && methodInfo.getArgType() == null)
                        || (m.getParameterTypes().length == 1 && m.getParameterTypes()[0].getName().equals(methodInfo.getArgType())))) {
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
            return "未找到对应函数 " + methodInfo.getName();
        }
        try {
            method.setAccessible(true);
            Object obj;
            if (methodInfo.getArgType() != null) {
                switch (methodInfo.getArgType()) {
                    case "int":
                        obj = method.invoke(view, Integer.valueOf(methodInfo.getArgValue()));
                        break;
                    case "boolean":
                        obj = method.invoke(view, Boolean.valueOf(methodInfo.getArgValue()));
                        break;
                    case "byte":
                        obj = method.invoke(view, Byte.valueOf(methodInfo.getArgValue()));
                        break;
                    case "float":
                        obj = method.invoke(view, Float.valueOf(methodInfo.getArgValue()));
                        break;
                    case "long":
                        obj = method.invoke(view, Long.valueOf(methodInfo.getArgValue()));
                        break;
                    case "double":
                        obj = method.invoke(view, Double.valueOf(methodInfo.getArgValue()));
                        break;
                    case "short":
                        obj = method.invoke(view, Short.valueOf(methodInfo.getArgValue()));
                        break;
                    case "char":
                        obj = method.invoke(view, methodInfo.getArgValue().toCharArray()[0]);
                        break;
                    case "java.lang.String":
                    case "java.lang.CharSequence":
                        obj = method.invoke(view, methodInfo.getArgValue());
                        break;
                    default:
                        obj = method.invoke(view);
                        break;
                }
            } else {
                obj = method.invoke(view);
            }
            if (obj != null) {
                return "true:" + obj;
            } else {
                return "true";
            }
        } catch (Throwable t) {
            result = Log.getStackTraceString(t);
        }
        return "调用函数失败, 失败原因: " + result;
    }

}
