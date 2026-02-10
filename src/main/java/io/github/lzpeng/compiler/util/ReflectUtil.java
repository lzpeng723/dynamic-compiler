package io.github.lzpeng.compiler.util;

import java.lang.reflect.*;
import java.util.*;

/**
 *
 * 反射工具类
 *
 * @author lzpeng723
 * @since 1.0.0-M4
 */
public class ReflectUtil {


    /**
     * 缓存类及其对应方法数组的映射关系。
     * 使用WeakHashMap实现，确保在类被垃圾回收时，对应的缓存条目也会被自动清理，
     * 避免内存泄漏问题。
     */
    private static final Map<Class<?>, Method[]> METHODS_CACHE = new WeakHashMap<>();

    /**
     * 缓存字段信息的静态映射表。
     * <p>
     * 该映射表用于存储类与其对应字段数组之间的映射关系，以提高反射操作的性能。
     * 使用WeakHashMap实现，确保在类被垃圾回收时，对应的缓存条目也能被自动清理，
     * 避免内存泄漏问题。
     */
    private static final Map<Class<?>, Field[]> FIELDS_CACHE = new WeakHashMap<>();

    /**
     * 缓存类构造器的映射表。
     * <p>
     * 该静态常量用于存储类与其对应构造器数组之间的映射关系。
     * 使用WeakHashMap实现，确保在类不再被引用时能够自动清理缓存，
     * 避免内存泄漏问题。
     * <p>
     * 键：Class<?> 类型，表示目标类的Class对象。
     * 值：Constructor<?>[] 类型，表示该类的所有构造器数组。
     */
    private static final Map<Class<?>, Constructor<?>[]> CONSTRUCTORS_CACHE = new WeakHashMap<>();

    /**
     * 动态加载指定名称的类。
     * <p>
     * 该方法首先尝试使用当前类的类加载器来加载指定名称的类。如果失败，则尝试使用当前线程的上下文类加载器来加载该类。
     *
     * @param <T>       泛型类型，表示要加载的类的类型
     * @param className 要加载的类的全限定名
     * @return 返回与给定字符串名称对应的Class对象
     */
    public static <T> Class<T> loadClass(String className) throws ClassNotFoundException {
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
    public static <T> Class<T> loadClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        @SuppressWarnings("unchecked") final Class<T> clazz = (Class<T>) classLoader.loadClass(className);
        return clazz;
    }

    /**
     * 获得一个类中所有字段列表，包括其父类中的字段<br>
     * 如果子类与父类中存在同名字段，则这两个字段同时存在，子类字段在前，父类字段在后。
     *
     * @param beanClass 类
     * @return 字段列表
     * @throws SecurityException 安全检查异常
     */
    public static Field[] getFields(Class<?> beanClass) throws SecurityException {
        Objects.requireNonNull(beanClass, "beanClass 不能为空");
        return FIELDS_CACHE.computeIfAbsent(beanClass, (key) -> getFieldsDirectly(beanClass, true));
    }

    /**
     * 获得一个类中所有字段列表，直接反射获取，无缓存<br>
     * 如果子类与父类中存在同名字段，则这两个字段同时存在，子类字段在前，父类字段在后。
     *
     * @param searchType           类
     * @param withSuperClassFields 是否包括父类的字段列表
     * @return 字段列表
     * @throws SecurityException 安全检查异常
     */
    public static Field[] getFieldsDirectly(Class<?> searchType, boolean withSuperClassFields) throws SecurityException {
        final List<Field> allFieldList = new ArrayList<>();
        do {
            final Field[] declaredFields = searchType.getDeclaredFields();
            Collections.addAll(allFieldList, declaredFields);
            searchType = withSuperClassFields ? searchType.getSuperclass() : null;
        } while (searchType != null);
        return allFieldList.toArray(new Field[0]);
    }

    /**
     * 获取给定对象中指定名称的字段。
     *
     * @param obj       对象实例，从中查找字段
     * @param fieldName 字段名
     * @return 返回找到的Field对象，如果找不到则返回null
     */
    public static Field getField(Object obj, String fieldName) {
        Objects.requireNonNull(obj, "obj is null");
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
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(fieldName);
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
     * @param <T>       字段值的类型
     * @param clazz     对象实例，从中获取字段值
     * @param fieldName 字段名
     * @return 返回指定字段的值，如果找不到该字段或对象为空则返回null
     */
    public static <T> T getFieldValue(Class<?> clazz, String fieldName) {
        final Field field = ReflectUtil.getField(clazz, fieldName);
        return ReflectUtil.getFieldValue(clazz, field);
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
        Objects.requireNonNull(obj);
        Objects.requireNonNull(field);
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
        Objects.requireNonNull(method);
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
    public static <T> T invokeMethod(Object obj, Method method, Object... params) {
        Objects.requireNonNull(method);
        ReflectUtil.setAccessible(method);
        try {
            if (Modifier.isStatic(method.getModifiers())) {
                @SuppressWarnings("unchecked") final T returnObj = (T) method.invoke(null, NullWrap.getValue(params));
                return returnObj;
            } else {
                @SuppressWarnings("unchecked") final T returnObj = (T) method.invoke(obj, NullWrap.getValue(params));
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
     * 获得一个类中所有方法列表，包括其父类中的方法
     *
     * @param beanClass 类，非{@code null}
     * @return 方法列表
     * @throws SecurityException 安全检查异常
     */
    public static Method[] getMethods(Class<?> beanClass) throws SecurityException {
        Objects.requireNonNull(beanClass);
        return METHODS_CACHE.computeIfAbsent(beanClass,
                (key) -> getMethodsDirectly(beanClass, true, true));
    }

    /**
     * 获得一个类中所有方法列表，直接反射获取，无缓存<br>
     * 接口获取方法和默认方法，获取的方法包括：
     * <ul>
     *     <li>本类中的所有方法（包括static方法）</li>
     *     <li>父类中的所有方法（包括static方法）</li>
     *     <li>Object中（包括static方法）</li>
     * </ul>
     *
     * @param beanClass            类或接口
     * @param withSupers           是否包括父类或接口的方法列表
     * @param withMethodFromObject 是否包括Object中的方法
     * @return 方法列表
     * @throws SecurityException 安全检查异常
     */
    public static Method[] getMethodsDirectly(Class<?> beanClass, boolean withSupers, boolean withMethodFromObject) throws SecurityException {
        Objects.requireNonNull(beanClass);
        if (beanClass.isInterface()) {
            // 对于接口，直接调用Class.getMethods方法获取所有方法，因为接口都是public方法
            return withSupers ? beanClass.getMethods() : beanClass.getDeclaredMethods();
        }

        final List<Method> methodList = new ArrayList<>();
        Class<?> searchType = beanClass;
        while (searchType != null) {
            if (!withMethodFromObject && Object.class == searchType) {
                break;
            }
            Collections.addAll(methodList, searchType.getDeclaredMethods());
            methodList.addAll(getDefaultMethodsFromInterface(searchType));
            searchType = (withSupers && !searchType.isInterface()) ? searchType.getSuperclass() : null;
        }
        return methodList.toArray(new Method[0]);
    }

    /**
     * 获取类对应接口中的非抽象方法（default方法）
     *
     * @param clazz 类
     * @return 方法列表
     */
    private static List<Method> getDefaultMethodsFromInterface(Class<?> clazz) {
        List<Method> result = new ArrayList<>();
        for (Class<?> ifc : clazz.getInterfaces()) {
            for (Method m : ifc.getMethods()) {
                if (!Modifier.isAbstract(m.getModifiers())) {
                    result.add(m);
                }
            }
        }
        return result;
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
        Objects.requireNonNull(obj);
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
        final Method[] methods = ReflectUtil.getMethods(clazz);
        M:
        for (Method method : methods) {
            if (!Objects.equals(method.getName(), methodName)) {
                continue;
            }
            if (method.getParameterCount() != params.length) {
                continue;
            }
            final Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                final Object param = params[i];
                final Class<?> paramType = parameterTypes[i];
                final Class<?> inputParamType = NullWrap.getClazz(param);
                if (!paramType.isAssignableFrom(inputParamType)) {
                    continue M;
                }
            }
            return method;
        }
        return null;
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
        Objects.requireNonNull(obj);
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
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(methodName);
        final Queue<Class<?>> queue = new LinkedList<>();
        queue.offer(clazz);
        while (!queue.isEmpty()) {
            final Class<?> currentClazz = queue.poll();
            try {
                return clazz.getDeclaredMethod(methodName, classes);
            } catch (NoSuchMethodException e) {
                final Class<?> superclass = currentClazz.getSuperclass();
                if (superclass != null) {
                    queue.offer(superclass);
                }
                final Class<?>[] interfaces = currentClazz.getInterfaces();
                for (Class<?> anInterface : interfaces) {
                    queue.offer(anInterface);
                }
            }
        }
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
            return constructor.newInstance(NullWrap.getValue(params));
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
    public static <T> T newInstance(ClassLoader classLoader, String className, Object... params) throws ClassNotFoundException {
        final Class<T> clazz = ReflectUtil.loadClass(classLoader, className);
        return ReflectUtil.newInstance(clazz, params);
    }


    /**
     * 获得一个类中所有构造列表
     *
     * @param <T>       构造的对象类型
     * @param beanClass 类，非{@code null}
     * @return 字段列表
     * @throws SecurityException 安全检查异常
     */
    @SuppressWarnings("unchecked")
    public static <T> Constructor<T>[] getConstructors(Class<T> beanClass) throws SecurityException {
        Objects.requireNonNull(beanClass);
        return (Constructor<T>[]) CONSTRUCTORS_CACHE.computeIfAbsent(beanClass, (key) -> getConstructorsDirectly(beanClass));
    }

    /**
     * 获得一个类中所有构造列表，直接反射获取，无缓存
     *
     * @param beanClass 类
     * @return 字段列表
     * @throws SecurityException 安全检查异常
     */
    public static Constructor<?>[] getConstructorsDirectly(Class<?> beanClass) throws SecurityException {
        return beanClass.getDeclaredConstructors();
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
        final Constructor<T>[] constructors = ReflectUtil.getConstructors(clazz);
        CONSTRUCTOR:
        for (Constructor<T> constructor : constructors) {
            if (constructor.getParameterCount() != params.length) {
                continue;
            }
            final Class<?>[] parameterTypes = constructor.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                final Object param = params[i];
                final Class<?> paramType = parameterTypes[i];
                final Class<?> inputParamType = NullWrap.getClazz(param);
                if (!inputParamType.isAssignableFrom(paramType)) {
                    continue CONSTRUCTOR;
                }
            }
            return constructor;
        }
        return null;
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
        Objects.requireNonNull(clazz);
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

    /**
     * 创建并返回一个指定类的 NullWrap 实例。
     *
     * @param clazz 指定的类对象，用于创建对应的 NullWrap 实例
     * @return 返回一个新的 NullWrap 实例，该实例与传入的类相关联
     */
    public static NullWrap nullWrap(Class<?> clazz) {
        return NullWrap.newInstance(clazz);
    }

    /**
     * NullWrap 是一个用于包装类信息的内部静态类。
     * 它主要用于处理对象可能为 null 的情况，并提供获取对象类型或值的方法。
     */
    private static class NullWrap {
        /**
         * 存储被包装的类信息。
         */
        private final Class<?> clazz;

        /**
         * 构造函数，初始化 NullWrap 实例。
         *
         * @param clazz 被包装的类信息
         */
        private NullWrap(Class<?> clazz) {
            this.clazz = clazz;
        }

        /**
         * 获取被包装的类信息。
         *
         * @return 被包装的类信息
         */
        public Class<?> getClazz() {
            return clazz;
        }

        /**
         * 获取对象的类信息。如果对象为 null，则返回 null；
         * 如果对象是 NullWrap 类型，则返回其封装的类信息；
         * 否则返回对象的实际类信息。
         *
         * @param obj 输入对象
         * @return 对象的类信息
         */
        private static Class<?> getClazz(Object obj) {
            if (obj == null) {
                return null;
            }
            if (obj instanceof NullWrap) {
                return ((NullWrap) obj).getClazz();
            }
            return obj.getClass();
        }

        /**
         * 批量获取对象数组中每个元素的类信息。
         *
         * @param objs 对象数组
         * @return 类信息数组
         */
        private static Class<?>[] getClazz(Object... objs) {
            final Class<?>[] classes = new Class[objs.length];
            for (int i = 0; i < objs.length; i++) {
                classes[i] = getClazz(objs[i]);
            }
            return classes;
        }

        /**
         * 获取对象的实际值。如果对象是 NullWrap 类型，则返回 null；
         * 否则返回对象本身。
         *
         * @param obj 输入对象
         * @return 对象的实际值
         */
        private static Object getValue(Object obj) {
            if (obj instanceof NullWrap) {
                return null;
            }
            return obj;
        }

        /**
         * 批量获取对象数组中每个元素的实际值。
         *
         * @param objs 对象数组
         * @return 处理后的对象数组
         */
        private static Object[] getValue(Object... objs) {
            for (int i = 0; i < objs.length; i++) {
                objs[i] = getValue(objs[i]);
            }
            return objs;
        }

        /**
         * 创建一个新的 NullWrap 实例。
         *
         * @param clazz 需要包装的类信息
         * @return 新的 NullWrap 实例
         */
        private static NullWrap newInstance(Class<?> clazz) {
            return new NullWrap(clazz);
        }
    }
}
