package com.bytedance.tools.codelocator.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;

public class ReflectUtils {

    private static HashMap<String, Field> sCacheFiled = new HashMap<>();

    private static HashMap<String, Method> sCacheMethod = new HashMap<>();

    private static Method sGetDeclaredField = null;

    private static Method sGetDeclaredMethod = null;

    static {
        try {
            sGetDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);
            sGetDeclaredMethod.setAccessible(true);
        } catch (Throwable t) {
        }
        try {
            sGetDeclaredField = Class.class.getDeclaredMethod("getDeclaredField", java.lang.String.class);
            sGetDeclaredField.setAccessible(true);
        } catch (Throwable t) {
        }
    }

    public static Field getClassField(Class clz, String fieldName) {
        Field declaredField = null;
        final String cacheKey = clz.getName() + "." + fieldName;
        if (sCacheFiled.containsKey(cacheKey)) {
            return sCacheFiled.get(cacheKey);
        }
        while (clz != null && clz != Object.class) {
            try {
                if (sGetDeclaredField != null) {
                    declaredField = (Field) sGetDeclaredField.invoke(clz, fieldName);
                } else {
                    declaredField = clz.getDeclaredField(fieldName);
                }
                declaredField.setAccessible(true);
            } catch (Throwable ignore) {
            }
            if (declaredField != null) {
                sCacheFiled.put(cacheKey, declaredField);
                return declaredField;
            }
            clz = clz.getSuperclass();
        }
        sCacheFiled.put(cacheKey, null);
        return null;
    }

    public static Method getClassMethod(Class clz, String methodName) {
        return getClassMethod(clz, methodName, (Class[]) null);
    }

    public static Method getClassMethod(Class clz, String methodName, Class<?>... clzs) {
        if (clz == null || methodName == null) {
            return null;
        }
        Method declaredMethod = null;
        final String cacheKey = clz.getName() + "." + methodName;
        if (sCacheMethod.containsKey(cacheKey)) {
            return sCacheMethod.get(cacheKey);
        }
        while (clz != null && clz != Object.class) {
            try {
                if (sGetDeclaredMethod != null) {
                    declaredMethod = (Method) sGetDeclaredMethod.invoke(clz, methodName, clzs);
                } else {
                    declaredMethod = (Method) clz.getDeclaredMethod(methodName, clzs);
                }
                declaredMethod.setAccessible(true);
            } catch (Throwable ignore) {
            }
            if (declaredMethod != null) {
                sCacheMethod.put(cacheKey, declaredMethod);
                return declaredMethod;
            }
            clz = clz.getSuperclass();
        }
        sCacheMethod.put(cacheKey, null);
        return null;
    }

    public static HashSet<String> SUPPORT_ARGS = new HashSet<String>() {
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
        return Modifier.isStatic(field.getModifiers()) || !SUPPORT_ARGS.contains(field.getType().getName());
    }

}
