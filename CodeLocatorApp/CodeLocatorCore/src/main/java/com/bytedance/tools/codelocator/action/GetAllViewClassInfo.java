package com.bytedance.tools.codelocator.action;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.tools.codelocator.model.FieldInfo;
import com.bytedance.tools.codelocator.model.MethodInfo;
import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.model.ViewClassInfo;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;
import com.bytedance.tools.codelocator.utils.GsonUtils;
import com.bytedance.tools.codelocator.utils.ReflectUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by liujian.android on 2024/4/1
 *
 * @author liujian.android@bytedance.com
 */
public class GetAllViewClassInfo extends ViewAction {

    @NonNull
    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.GET_VIEW_CLASS_INFO;
    }

    @Override
    public void processViewAction(@NonNull View view, String data, @NonNull ResultData result) {
        HashSet<FieldInfo> fieldSet = new HashSet<>();
        Class javaClass = view.getClass();
        while (javaClass != Object.class) {
            try {
                final Field[] declaredFields = javaClass.getDeclaredFields();
                for (Field field : declaredFields) {
                    FieldInfo fieldInfo = getFieldInfo(view, field);
                    if (fieldInfo == null) {
                        continue;
                    }
                    fieldSet.add(fieldInfo);
                }
            } catch (Throwable t) {
            }
            javaClass = javaClass.getSuperclass();
        }
        List<MethodInfo> allMethodInfo = getAllMethodInfo(view.getClass());
        HashMap<String, MethodInfo> methodMap = new HashMap<>();
        for (MethodInfo methodInfo : allMethodInfo) {
            if ("boolean".equalsIgnoreCase(methodInfo.getReturnType()) && methodInfo.getName().startsWith("is")) {
                String fieldName = methodInfo.getName().substring("is".length());
                methodMap.put(methodInfo.getName(), methodInfo);
                if (methodMap.containsKey("set" + fieldName)) {
                    addMockField(
                            view,
                            fieldName,
                            methodInfo.getReturnType(),
                            fieldSet,
                            methodInfo.getMethod()
                    );
                }
            } else if (methodInfo.getName().startsWith("get")) {
                String fieldName = methodInfo.getName().substring("get".length());
                methodMap.put(methodInfo.getName(), methodInfo);
                if (methodMap.containsKey("set" + fieldName)) {
                    addMockField(
                            view,
                            fieldName,
                            methodInfo.getReturnType(),
                            fieldSet,
                            methodInfo.getMethod()
                    );
                }
            } else if (methodInfo.getName().startsWith("set")) {
                String fieldName = methodInfo.getName().substring("get".length());
                methodMap.put(methodInfo.getName(), methodInfo);
                if (methodMap.containsKey("get" + fieldName)) {
                    addMockField(
                            view,
                            fieldName,
                            methodInfo.getArgType(),
                            fieldSet,
                            methodMap.get("get" + fieldName).getMethod()
                    );
                }
            }
        }
        ViewClassInfo viewClassInfo = new ViewClassInfo();
        final ArrayList<FieldInfo> fieldInfoList = new ArrayList<>();
        fieldInfoList.addAll(fieldSet);
        viewClassInfo.setFieldInfoList(fieldInfoList);
        viewClassInfo.setMethodInfoList(allMethodInfo);
        result.addResultItem(CodeLocatorConstants.ResultKey.DATA, GsonUtils.sGson.toJson(viewClassInfo));
    }

    private void addMockField(View view, String fieldName, String type, HashSet<FieldInfo> fieldSet, Method method) {
        FieldInfo toRemove = null;
        for (FieldInfo fieldInfo : fieldSet) {
            if (("m" + fieldName).equals(fieldInfo.getName())) {
                toRemove = fieldInfo;
                break;
            }
        }
        if (toRemove != null) {
            fieldSet.remove(toRemove);
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


    private @Nullable
    MethodInfo getMethodInfo(Method method) {
        if (method.getParameterTypes().length > 1) {
            return null;
        }
        MethodInfo methodInfo = new MethodInfo();
        if (method.getParameterTypes().length == 1) {
            if (!ReflectUtils.SUPPORT_ARGS.contains(method.getParameterTypes()[0].getName())) {
                return null;
            }
            methodInfo.setArgType(method.getParameterTypes()[0].getName());
        }
        methodInfo.setMethod(method);
        methodInfo.setReturnType(method.getReturnType().getName());
        methodInfo.setName(method.getName());
        return methodInfo;
    }

    private @Nullable
    FieldInfo getFieldInfo(View view, Field field) {
        if (ReflectUtils.isLegalField(field)) {
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
        fieldInfo.setEditable(!Modifier.isFinal(field.getModifiers()));
        fieldInfo.setType(field.getType().getName());
        return fieldInfo;
    }

    private List<MethodInfo> getAllMethodInfo(Class clazz) {
        ArrayList<MethodInfo> list = new ArrayList<>();
        Class javaClass = clazz;
        while (javaClass != Object.class) {
            try {
                final Method[] declaredMethods = javaClass.getDeclaredMethods();
                for (Method method : declaredMethods) {
                    MethodInfo methodInfo = getMethodInfo(method);
                    if (methodInfo == null) {
                        continue;
                    }
                    if (list.contains(methodInfo)) {
                        continue;
                    }
                    list.add(methodInfo);
                }
            } catch (Throwable t) {
            }
            javaClass = javaClass.getSuperclass();
        }
        return list;
    }

}
