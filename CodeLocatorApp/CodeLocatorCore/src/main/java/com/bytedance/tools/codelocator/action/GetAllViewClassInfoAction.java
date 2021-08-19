package com.bytedance.tools.codelocator.action;

import android.view.View;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.model.FieldInfo;
import com.bytedance.tools.codelocator.model.MethodInfo;
import com.bytedance.tools.codelocator.model.ViewClassInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class GetAllViewClassInfoAction extends ViewAction {

    static Set<String> SUPPORT_ARGS = new HashSet<String>() {
        {
            add("int");
            add("boolean");
            add("byte");
            add("float");
            add("long");
            add("double");
            add("short");
            add("char");
            add("java.lang.String");
            add("java.lang.CharSequence");
        }
    };

    public static boolean isLegalField(Field field) {
        return !Modifier.isStatic(field.getModifiers()) && SUPPORT_ARGS.contains(field.getType().getName());
    }

    @Override
    public String getActionType() {
        return "GAC";
    }

    private MethodInfo getMethodInfo(Method method) {
        if (method.getParameterTypes().length > 1) {
            return null;
        }
        MethodInfo methodInfo = new MethodInfo();
        if (method.getParameterTypes().length == 1) {
            if (!SUPPORT_ARGS.contains(method.getParameterTypes()[0].getName())) {
                return null;
            }
            methodInfo.setArgType(method.getParameterTypes()[0].getName());
        }
        methodInfo.setMethod(method);
        methodInfo.setReturnType(method.getReturnType().getName());
        methodInfo.setName(method.getName());
        return methodInfo;
    }

    private FieldInfo getFieldInfo(View view, Field field) {
        if (!isLegalField(field)) {
            return null;
        }
        FieldInfo fieldInfo = new FieldInfo();
        try {
            field.setAccessible(true);
            fieldInfo.setValue("" + field.get(view));
        } catch (Throwable ignore) {
            return null;
        }
        fieldInfo.setName(field.getName());
        fieldInfo.setEditable(Modifier.isFinal(field.getModifiers()));
        fieldInfo.setType(field.getType().getName());
        return fieldInfo;
    }

    private List<MethodInfo> getAllMethodInfo(Class clazz) {
        List<MethodInfo> list = new LinkedList<>();
        while (clazz != Object.class) {
            Method[] declaredMethods = clazz.getDeclaredMethods();
            for (Method method : declaredMethods) {
                MethodInfo methodInfo = getMethodInfo(method);
                if (methodInfo == null || list.contains(methodInfo)) {
                    continue;
                }
                list.add(methodInfo);
            }
            clazz = clazz.getSuperclass();
        }
        return list;
    }

    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
        HashSet<FieldInfo> fieldSet = new HashSet<>();
        Class clazz = view.getClass();
        while (clazz != Object.class) {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field field : declaredFields) {
                FieldInfo fieldInfo = getFieldInfo(view, field);
                if (fieldInfo == null) {
                    continue;
                }
                fieldSet.add(fieldInfo);
            }
            clazz = clazz.getSuperclass();
        }
        List<MethodInfo> allMethodInfo = getAllMethodInfo(view.getClass());
        HashMap<String, MethodInfo> methodMap = new HashMap<>();
        for (MethodInfo methodInfo : allMethodInfo) {
            if ("boolean" == methodInfo.getReturnType() && methodInfo.getName().startsWith("is")) {
                String fieldName = methodInfo.getName().substring("is".length());
                methodMap.put(methodInfo.getName(), methodInfo);
                if (methodMap.containsKey("set" + fieldName)) {
                    addMockField(view, fieldName, methodInfo.getReturnType(), fieldSet, methodInfo.getMethod());
                }
            } else if (methodInfo.getName().startsWith("get")) {
                String fieldName = methodInfo.getName().substring("get".length());
                methodMap.put(methodInfo.getName(), methodInfo);
                if (methodMap.containsKey("set" + fieldName)) {
                    addMockField(view, fieldName, methodInfo.getReturnType(), fieldSet, methodInfo.getMethod());
                }
            } else if (methodInfo.getName().startsWith("set")) {
                String fieldName = methodInfo.getName().substring("get".length());
                methodMap.put(methodInfo.getName(), methodInfo);
                if (methodMap.containsKey("get" + fieldName)) {
                    addMockField(view, fieldName, methodInfo.getArgType(), fieldSet, methodMap.get("get" + fieldName).getMethod());
                }
            }
        }
        ViewClassInfo viewClassInfo = new ViewClassInfo();
        viewClassInfo.setFieldInfoList(new LinkedList<>(fieldSet));
        viewClassInfo.setMethodInfoList(allMethodInfo);
        resultSb.append(CodeLocator.sGson.toJson(viewClassInfo));
    }

    private void addMockField(View view, String fieldName, String type, HashSet<FieldInfo> fieldSet, Method method) {
        final Iterator<FieldInfo> iterator = fieldSet.iterator();
        while (iterator.hasNext()) {
            final FieldInfo next = iterator.next();
            if (("m" + fieldName).equals(next.getName())) {
                iterator.remove();
                break;
            }
        }
        FieldInfo fieldInfo = new FieldInfo();
        try {
            method.setAccessible(true);
            fieldInfo.setValue("" + method.invoke(view));
        } catch (Throwable t) {
            return;
        }
        fieldInfo.setName(fieldName);
        fieldInfo.setType(type);
        fieldInfo.setIsMethod(true);
        fieldSet.add(fieldInfo);
    }

}