package io.github.lzpeng.compiler.util;

import lombok.SneakyThrows;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 * 反射工具类
 *
 * @author lzpeng723
 * @date 2026-02-03 13:47 星期二
 */
public class ReflectUtil {

    @SneakyThrows
    private static Field getField(Object obj, String fieldName) {
        if (Objects.isNull(obj)) {
            return null;
        }
        return ReflectUtil.getField(obj.getClass(), fieldName);
    }


    @SneakyThrows
    private static Field getField(Class clazz, String fieldName) {
        if (Objects.isNull(clazz)) {
            return null;
        }
        if (Objects.isNull(fieldName)) {
            return null;
        }
        do {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        } while (clazz != null);
        return null;
    }


    @SneakyThrows
    public static void setFieldValue(Object obj, String fieldName, Object fieldValue) {
        final Field field = ReflectUtil.getField(obj, fieldName);
        ReflectUtil.setFieldValue(obj, field, fieldValue);
    }


    @SneakyThrows
    public static void setFieldValue(Object obj, Field field, Object fieldValue) {
        if (field == null) {
            return;
        }
        ReflectUtil.setAccessible(field);
        field.set(obj, fieldValue);
    }


    @SneakyThrows
    public static <T> T getFieldValue(Object obj, String fieldName) {
        final Field field = ReflectUtil.getField(obj, fieldName);
        return ReflectUtil.getFieldValue(obj, field);
    }

    @SneakyThrows
    public static <T> T getFieldValue(Object obj, Field field) {
        if (Objects.isNull(field)) {
            return null;
        }
        if (Objects.isNull(obj)) {
            return null;
        }
        ReflectUtil.setAccessible(field);
        return (T) field.get(obj);
    }

    @SneakyThrows
    private static Object invokeMethod(Object obj, Method method, Object[] params) {
        if (method == null) {
            return null;
        }
        ReflectUtil.setAccessible(method);
        return method.invoke(obj, params);
    }

    @SneakyThrows
    public static <T> T invokeMethod(Object obj, String methodName, Object... params) {
        final Method method = ReflectUtil.getMethod(obj, methodName, params);
        return (T) ReflectUtil.invokeMethod(obj, method, params);
    }

    @SneakyThrows
    private static Method getMethod(Object obj, String methodName, Object... params) {
        if (Objects.isNull(obj)) {
            return null;
        }
        return ReflectUtil.getMethod(obj.getClass(), methodName, params);
    }

    @SneakyThrows
    private static Method getMethod(Class clazz, String methodName, Object... params) {
        final Class[] classes = Arrays.stream(params).map(Object::getClass).toArray(Class[]::new);
        return ReflectUtil.getMethod(clazz, methodName, classes);
    }

    @SneakyThrows
    private static Method getMethod(Object obj, String methodName, Class... classes) {
        if (Objects.isNull(obj)) {
            return null;
        }
        return ReflectUtil.getMethod(obj.getClass(), methodName, classes);
    }

    @SneakyThrows
    private static Method getMethod(Class clazz, String methodName, Class... classes) {
        if (Objects.isNull(clazz)) {
            return null;
        }
        if (Objects.isNull(methodName)) {
            return null;
        }
        do {
            try {
                return clazz.getDeclaredMethod(methodName, classes);
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        } while (clazz != null);
        return null;
    }


    public static void setAccessible(AccessibleObject accessibleObject) {
        if (!accessibleObject.isAccessible()) {
            accessibleObject.setAccessible(true);
        }

    }
}
