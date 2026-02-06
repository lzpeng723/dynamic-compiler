package io.github.lzpeng.compiler.util;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 * 反射工具类
 *
 * @author lzpeng723
 * @since 1.0.0-M4
 */
public class ReflectUtil {

    /**
     * 动态加载指定名称的类。
     * <p>
     * 该方法首先尝试使用当前类的类加载器来加载指定名称的类。如果失败，则尝试使用当前线程的上下文类加载器来加载该类。
     *
     * @param <T>       泛型类型，表示要加载的类的类型
     * @param className 要加载的类的全限定名
     * @return 返回与给定字符串名称对应的Class对象
     */
    public static <T> Class<T> loadClass(String className) {
        try {
            return ReflectUtil.loadClass(ReflectUtil.class.getClassLoader(), className);
        } catch (Exception e) {
            return ReflectUtil.loadClass(Thread.currentThread().getContextClassLoader(), className);
        }
    }

    /**
     * 动态加载指定名称的类。
     *
     * @param <T>         泛型类型，表示要加载的类的类型
     * @param classLoader 用于加载类的类加载器
     * @param className   要加载的类的全限定名
     * @return 返回与给定字符串名称对应的Class对象
     */
    public static <T> Class<T> loadClass(ClassLoader classLoader, String className) {
        try {
            @SuppressWarnings("unchecked") final Class<T> clazz = (Class<T>) classLoader.loadClass(className);
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * 获取给定对象中指定名称的字段。
     *
     * @param obj       对象实例，从中查找字段
     * @param fieldName 字段名
     * @return 返回找到的Field对象，如果找不到则返回null
     */
    public static Field getField(Object obj, String fieldName) {
        if (Objects.isNull(obj)) {
            return null;
        }
        return ReflectUtil.getField(obj.getClass(), fieldName);
    }

    /**
     * 获取给定类中指定名称的字段。
     * <p>
     * 该方法尝试从提供的类及其所有父类中查找指定名称的字段。如果在当前类中找不到该字段，则会继续在其父类中查找，直到找到或遍历完所有父类为止。
     *
     * @param clazz     类对象，从中查找字段
     * @param fieldName 字段名
     * @return 返回找到的Field对象，如果找不到则返回null
     */
    public static Field getField(Class<?> clazz, String fieldName) {
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
        throw new IllegalArgumentException(new NoSuchFieldException(fieldName));
    }

    /**
     * 设置指定对象中给定字段名的字段值。
     * <p>
     * 该方法首先通过反射获取到指定对象中的指定字段，然后设置该字段的值为提供的新值。
     * 如果指定的对象为空或者无法找到指定名称的字段，则该操作不会执行。
     *
     * @param obj        对象实例，其字段将被设置
     * @param fieldName  字段名，表示要设置值的字段
     * @param fieldValue 新的字段值，将被设置到指定字段上
     */
    public static void setFieldValue(Object obj, String fieldName, Object fieldValue) {
        final Field field = ReflectUtil.getField(obj, fieldName);
        ReflectUtil.setFieldValue(obj, field, fieldValue);
    }


    /**
     * 设置指定对象中给定字段的值。
     * <p>
     * 该方法通过反射机制来修改目标对象上指定字段的值。如果提供的字段对象为null，则此操作不会执行任何更改。
     * 在设置字段值之前，会先确保该字段是可访问的。
     *
     * @param obj        对象实例，其字段将被设置
     * @param field      字段对象，表示要设置值的字段
     * @param fieldValue 新的字段值，将被设置到指定字段上
     */
    public static void setFieldValue(Object obj, Field field, Object fieldValue) {
        if (field == null) {
            return;
        }
        ReflectUtil.setAccessible(field);
        try {
            field.set(obj, fieldValue);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /**
     * 通过反射获取指定对象中字段的值。
     *
     * @param <T>       字段值的类型
     * @param obj       对象实例，从中获取字段值
     * @param fieldName 字段名
     * @return 返回指定字段的值，如果找不到该字段或对象为空则返回null
     */
    public static <T> T getFieldValue(Object obj, String fieldName) {
        final Field field = ReflectUtil.getField(obj, fieldName);
        return ReflectUtil.getFieldValue(obj, field);
    }

    /**
     * 通过反射获取指定对象中字段的值。
     *
     * @param <T>   字段值的类型
     * @param obj   对象实例，从中获取字段值
     * @param field 字段对象，表示要获取值的字段
     * @return 返回指定字段的值，如果找不到该字段或对象为空则返回null
     */
    public static <T> T getFieldValue(Object obj, Field field) {
        if (Objects.isNull(field)) {
            return null;
        }
        if (Objects.isNull(obj)) {
            return null;
        }
        ReflectUtil.setAccessible(field);
        try {
            if (Modifier.isStatic(field.getModifiers())) {
                @SuppressWarnings("unchecked") final T fieldValue = (T) field.get(null);
                return fieldValue;
            } else {
                @SuppressWarnings("unchecked") final T fieldValue = (T) field.get(obj);
                return fieldValue;
            }
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * 通过反射调用指定类的静态方法。
     *
     * @param <T>    返回值类型
     * @param method 要调用的方法对象
     * @param params 方法参数数组
     * @return 静态方法的返回值，如果方法为null则返回null
     */
    public static <T> T invokeStaticMethod(Method method, Object[] params) {
        if (method == null) {
            return null;
        }
        ReflectUtil.setAccessible(method);
        try {
            @SuppressWarnings("unchecked") final T returnObj = (T) method.invoke(null, params);
            return returnObj;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * 通过反射调用指定类的静态方法。
     *
     * @param <T>        返回值类型
     * @param obj        要调用静态方法的对象
     * @param methodName 方法名
     * @param params     方法参数数组
     * @return 静态方法的返回值，如果方法为null则返回null
     */
    public static <T> T invokeStaticMethod(Object obj, String methodName, Object... params) {
        return ReflectUtil.invokeStaticMethod(obj.getClass(), methodName, params);
    }

    /**
     * 通过反射调用指定类的静态方法。
     *
     * @param <T>        返回值类型
     * @param clazz      要调用静态方法的类
     * @param methodName 方法名
     * @param params     方法参数数组
     * @return 静态方法的返回值，如果方法为null则返回null
     */
    public static <T> T invokeStaticMethod(Class<?> clazz, String methodName, Object... params) {
        final Method method = ReflectUtil.getMethod(clazz, methodName, params);
        return ReflectUtil.invokeStaticMethod(method, params);
    }

    /**
     * 通过反射调用指定对象的方法。
     *
     * @param <T>    泛型类型，表示方法的返回值类型
     * @param obj    要调用方法的对象实例。对于静态方法，此参数可以为null。
     * @param method 要调用的方法对象
     * @param params 方法调用时需要传递的参数列表
     * @return 方法调用的结果，如果方法为null则返回null
     */
    public static <T> T invokeMethod(Object obj, Method method, Object[] params) {
        if (method == null) {
            return null;
        }
        ReflectUtil.setAccessible(method);
        try {
            if (Modifier.isStatic(method.getModifiers())) {
                @SuppressWarnings("unchecked") final T returnObj = (T) method.invoke(null, params);
                return returnObj;
            } else {
                @SuppressWarnings("unchecked") final T returnObj = (T) method.invoke(obj, params);
                return returnObj;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * 通过反射调用指定对象的方法。
     *
     * @param <T>        泛型类型，表示方法的返回值类型
     * @param obj        要调用方法的对象实例
     * @param methodName 方法名
     * @param params     方法调用时需要传递的参数列表
     * @return 方法调用的结果
     */
    public static <T> T invokeMethod(Object obj, String methodName, Object... params) {
        final Method method = ReflectUtil.getMethod(obj, methodName, params);
        return ReflectUtil.invokeMethod(obj, method, params);
    }

    /**
     * 获取指定对象中具有给定名称和参数类型的方法。
     *
     * @param obj        要从中获取方法的对象实例
     * @param methodName 方法名
     * @param params     方法参数数组，用于确定方法的参数类型
     * @return 返回找到的Method对象；如果找不到匹配的方法或者提供的对象为null，则返回null
     */
    public static Method getMethod(Object obj, String methodName, Object... params) {
        if (Objects.isNull(obj)) {
            return null;
        }
        return ReflectUtil.getMethod(obj.getClass(), methodName, params);
    }

    /**
     * 获取指定类中具有给定名称和参数类型的方法。
     * <p>
     * 该方法尝试从提供的类及其所有父类中查找指定名称和参数类型的方法。如果在当前类中找不到该方法，则会继续在其父类中查找，直到找到或遍历完所有父类为止。
     *
     * @param clazz      要从中获取方法的类
     * @param methodName 方法名
     * @param params     参数数组，用于确定方法的参数类型
     * @return 返回找到的Method对象；如果找不到匹配的方法或者提供的类为null，则返回null
     */
    public static Method getMethod(Class<?> clazz, String methodName, Object... params) {
        final Class<?>[] classes = Arrays.stream(params).map(Object::getClass).toArray(Class[]::new);
        return ReflectUtil.getMethod(clazz, methodName, classes);
    }

    /**
     * 获取指定对象中具有给定名称和参数类型的方法。
     * <p>
     * 该方法首先检查提供的对象是否为null，如果是，则直接返回null。接着，它使用反射机制从对象的类及其所有父类中查找匹配的方法。
     * 如果在当前类中找不到该方法，则会继续在其父类中查找，直到找到或遍历完所有父类为止。
     *
     * @param obj        要从中获取方法的对象实例
     * @param methodName 方法名
     * @param classes    参数类型数组，用于确定方法的参数类型
     * @return 返回找到的Method对象；如果找不到匹配的方法或者提供的对象为null，则返回null
     */
    public static Method getMethod(Object obj, String methodName, Class<?>... classes) {
        if (Objects.isNull(obj)) {
            return null;
        }
        return ReflectUtil.getMethod(obj.getClass(), methodName, classes);
    }

    /**
     * 获取指定类中具有给定名称和参数类型的方法。
     * <p>
     * 该方法尝试从提供的类及其所有父类中查找指定名称和参数类型的方法。如果在当前类中找不到该方法，
     * 则会继续在其父类中查找，直到找到或遍历完所有父类为止。
     *
     * @param clazz      要从中获取方法的类
     * @param methodName 方法名
     * @param classes    参数类型数组，用于确定方法的参数类型
     * @return 返回找到的Method对象；如果找不到匹配的方法或者提供的类为null，则返回null
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... classes) {
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
        throw new IllegalArgumentException(new NoSuchMethodException(methodName));
    }


    /**
     * 使用给定的类和参数创建一个新的实例。
     * <p>
     * 该方法首先通过反射获取指定类中与给定参数类型匹配的构造函数，然后设置此构造函数为可访问状态，
     * 最后使用提供的参数调用此构造函数来创建并返回新的实例。
     *
     * @param <T>    泛型类型，表示要创建的实例的类型
     * @param clazz  要创建其实例的类
     * @param params 构造函数所需的参数
     * @return 返回根据给定类和参数创建的新实例
     */
    public static <T> T newInstance(Class<T> clazz, Object... params) {
        final Constructor<T> constructor = ReflectUtil.getConstructor(clazz, params);
        ReflectUtil.setAccessible(constructor);
        try {
            return constructor.newInstance(params);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * 使用给定的类加载器和类名创建一个新的实例。
     * <p>
     * 该方法首先通过提供的类加载器动态加载指定名称的类，然后使用反射机制根据给定的参数创建并返回该类的一个新实例。
     *
     * @param <T>         泛型类型，表示要创建的实例的类型
     * @param classLoader 用于加载类的类加载器
     * @param className   要加载的类的全限定名
     * @param params      创建实例时传递给构造函数的参数
     * @return 返回根据给定类名和参数创建的新实例
     */
    public static <T> T newInstance(ClassLoader classLoader, String className, Object... params) {
        final Class<T> clazz = ReflectUtil.loadClass(classLoader, className);
        return ReflectUtil.newInstance(clazz, params);
    }

    /**
     * 获取指定类中具有给定参数类型的构造函数。
     * <p>
     * 该方法尝试从提供的类中查找与给定参数类型匹配的构造函数。如果找不到匹配的构造函数，则返回null。
     * 如果提供的类为null，也会直接返回null。
     *
     * @param <T>    泛型类型，表示要获取构造函数的类的类型
     * @param clazz  要从中获取构造函数的类
     * @param params 构造函数所需的参数对象数组，用于确定构造函数的参数类型
     * @return 返回找到的Constructor对象；如果找不到匹配的构造函数或者提供的类为null，则返回null
     */
    public static <T> Constructor<T> getConstructor(Class<T> clazz, Object... params) {
        final Class<?>[] classes = Arrays.stream(params).map(Object::getClass).toArray(Class[]::new);
        return ReflectUtil.getConstructor(clazz, classes);
    }

    /**
     * 获取指定类中具有给定参数类型的构造函数。
     * <p>
     * 该方法尝试从提供的类中查找与给定参数类型匹配的构造函数。如果找不到匹配的构造函数，则返回null。
     * 如果提供的类为null，也会直接返回null。
     *
     * @param <T>     泛型类型，表示要获取构造函数的类的类型
     * @param clazz   要从中获取构造函数的类
     * @param classes 构造函数所需的参数类型数组
     * @return 返回找到的Constructor对象；如果找不到匹配的构造函数或者提供的类为null，则返回null
     */
    public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... classes) {
        if (Objects.isNull(clazz)) {
            return null;
        }
        try {
            return clazz.getDeclaredConstructor(classes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * 设置指定的AccessibleObject为可访问状态。
     * 如果给定的AccessibleObject当前不是可访问的，则将其设置为可访问。
     *
     * @param accessibleObject 要设置为可访问的AccessibleObject
     */
    public static void setAccessible(AccessibleObject accessibleObject) {
        if (!accessibleObject.isAccessible()) {
            accessibleObject.setAccessible(true);
        }

    }
}
