package io.github.lzpeng.compiler.util;

import io.github.lzpeng.compiler.JavaSourceCompiler;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;
import java.util.function.Function;

/**
 * 源码编译工具类，主要封装{@link JavaSourceCompiler} 相关功能
 *
 * @author lzpeng723
 * @since 1.0.0-M1
 */
public final class CompilerUtil {

    private static final String FUNCTION_CLASS_FORMAT = "%s public final class %s implements Function<Object[], Object> { public Object apply(Object[] params) { %s } }";


    /**
     * 创建Java源码编译器
     *
     * @return Java源码编译器
     */
    public static JavaSourceCompiler create() {
        return JavaSourceCompiler.create();
    }

    /**
     * 创建Java源码编译器
     *
     * @param parent 父类加载器
     * @return Java源码编译器
     */
    public static JavaSourceCompiler create(ClassLoader parent) {
        return JavaSourceCompiler.create(parent);
    }

    /**
     * 编译给定的Java源代码并返回对应的类加载器。
     *
     * @param className  类名，用于指定编译后的类名称
     * @param sourceCode 待编译的Java源代码字符串
     * @return 编译完成后生成的类加载器，通过该类加载器可以加载编译得到的类
     */
    public static ClassLoader compile(String className, String sourceCode) {
        return compile(null, className, sourceCode);
    }

    /**
     * 编译给定的Java源代码并返回对应的类加载器。
     *
     * @param parentClassLoader 类加载器
     * @param className         类名，用于指定编译后的类名称
     * @param sourceCode        待编译的Java源代码字符串
     * @return 编译完成后生成的类加载器，通过该类加载器可以加载编译得到的类
     */
    public static ClassLoader compile(ClassLoader parentClassLoader, String className, String sourceCode) {
        return create(parentClassLoader).addSource(className, sourceCode).compile();
    }

    /**
     * 编译给定的Java源代码文件或目录，并返回对应的类加载器。
     *
     * @param file 待编译的Java源代码文件或包含Java源代码文件的目录
     * @return 编译完成后生成的类加载器，通过该类加载器可以加载编译得到的类
     * @throws IllegalArgumentException 如果file为空、不存在或者无法读取
     */
    public static ClassLoader compile(File file) {
        if (file == null) {
            throw new IllegalArgumentException("文件为空");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("文件不存在 " + file.getAbsolutePath());
        }
        if (!file.canRead()) {
            throw new IllegalArgumentException("无法读取文件 " + file.getAbsolutePath());
        }
        if (file.isFile()) {
            return create().addSource(file).compile();
        }
        if (file.isDirectory()) {
            return create().addSourceDirectory(file).compile();
        }
        return null;
    }

    /**
     * 动态编译并加载指定类名和源代码的类。
     *
     * @param <T>        期望返回的类类型
     * @param className  类的全限定名
     * @param sourceCode 类的Java源代码字符串
     * @return 编译并加载后的类对象
     * @throws ClassNotFoundException 如果编译或加载过程中发生错误
     */
    public static <T> Class<T> loadClass(String className, String sourceCode) throws ClassNotFoundException {
        return loadClass(null, className, sourceCode);
    }

    /**
     * 动态编译并加载指定类名和源代码的类。
     *
     * @param <T>        期望返回的类类型
     * @param className  类的全限定名
     * @param sourceCode 类的Java源代码字符串
     * @return 编译并加载后的类对象
     * @throws ClassNotFoundException 如果编译或加载过程中发生错误
     */
    public static <T> Class<T> loadClass(ClassLoader parentClassLoader, String className, String sourceCode) throws ClassNotFoundException {
        final ClassLoader classLoader = CompilerUtil.compile(parentClassLoader, className, sourceCode);
        return ReflectUtil.loadClass(classLoader, className);
    }

    /**
     * 动态编译给定的Java源代码并创建指定类的新实例。
     *
     * @param <T>        期望返回的类类型
     * @param className  类的全限定名
     * @param sourceCode 类的Java源代码字符串
     * @param params     构造函数参数，用于初始化新创建的对象
     * @return 根据提供的类名和源代码动态编译后创建的类的新实例
     * @throws ClassNotFoundException 如果编译、加载类或创建实例过程中发生错误
     */
    public static <T> T newInstance(String className, String sourceCode, Object... params) throws ClassNotFoundException {
        return newInstance(null, className, sourceCode, params);
    }

    /**
     * 动态编译给定的Java源代码并创建指定类的新实例。
     *
     * @param <T>        期望返回的类类型
     * @param className  类的全限定名
     * @param sourceCode 类的Java源代码字符串
     * @param params     构造函数参数，用于初始化新创建的对象
     * @return 根据提供的类名和源代码动态编译后创建的类的新实例
     * @throws ClassNotFoundException 如果编译、加载类或创建实例过程中发生错误
     */
    public static <T> T newInstance(ClassLoader parentClassLoader, String className, String sourceCode, Object... params) throws ClassNotFoundException {
        final Class<T> clazz = CompilerUtil.loadClass(parentClassLoader, className, sourceCode);
        return ReflectUtil.newInstance(clazz, params);
    }

    /**
     * 调用指定类中的静态方法。
     *
     * @param <T>        返回值类型
     * @param className  类的全限定名
     * @param sourceCode 类的Java源代码字符串
     * @param methodName 需要调用的方法名称
     * @param params     传递给方法的参数列表
     * @return 方法执行后的返回值
     * @throws ClassNotFoundException 如果编译、加载类或方法调用过程中发生错误
     */
    public static <T> T invokeStaticMethod(String className, String sourceCode, String methodName, Object... params) throws ClassNotFoundException {
        return invokeStaticMethod(null, className, sourceCode, methodName, params);
    }

    /**
     * 调用指定类中的静态方法。
     *
     * @param <T>        返回值类型
     * @param className  类的全限定名
     * @param sourceCode 类的Java源代码字符串
     * @param methodName 需要调用的方法名称
     * @param params     传递给方法的参数列表
     * @return 方法执行后的返回值
     * @throws ClassNotFoundException 如果编译、加载类或方法调用过程中发生错误
     */
    public static <T> T invokeStaticMethod(ClassLoader parentClassLoader, String className, String sourceCode, String methodName, Object... params) throws ClassNotFoundException {
        final Class<T> clazz = CompilerUtil.loadClass(parentClassLoader, className, sourceCode);
        final Method method = ReflectUtil.getMethod(clazz, methodName, params);
        if (Modifier.isStatic(method.getModifiers())) {
            return ReflectUtil.invokeMethod(null, method, params);
        }
        throw new IllegalArgumentException(method + " is not static");
    }


    /**
     * 动态编译给定的Java源代码并调用指定类中的实例方法。
     *
     * @param <T>        返回值类型
     * @param className  类的全限定名
     * @param sourceCode 类的Java源代码字符串
     * @param methodName 需要调用的方法名称
     * @param params     传递给方法的参数列表
     * @return 方法执行后的返回值
     * @throws ClassNotFoundException 如果编译、加载类、创建实例或方法调用过程中发生错误
     */
    public static <T> T invokeInstanceMethod(String className, String sourceCode, String methodName, Object... params) throws ClassNotFoundException {
        return invokeInstanceMethod(null, className, sourceCode, methodName, params);
    }

    /**
     * 动态编译给定的Java源代码并调用指定类中的实例方法。
     *
     * @param <T>        返回值类型
     * @param className  类的全限定名
     * @param sourceCode 类的Java源代码字符串
     * @param methodName 需要调用的方法名称
     * @param params     传递给方法的参数列表
     * @return 方法执行后的返回值
     * @throws ClassNotFoundException 如果编译、加载类、创建实例或方法调用过程中发生错误
     */
    public static <T> T invokeInstanceMethod(ClassLoader parentClassLoader, String className, String sourceCode, String methodName, Object... params) throws ClassNotFoundException {
        final Class<T> clazz = CompilerUtil.loadClass(parentClassLoader, className, sourceCode);
        final Method method = ReflectUtil.getMethod(clazz, methodName, params);
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException(method + " is static");
        }
        final T obj = ReflectUtil.newInstance(clazz);
        return ReflectUtil.invokeMethod(obj, method, params);
    }


    /**
     * 执行给定的Java源代码并返回结果。
     *
     * @param <R>        返回值类型
     * @param sourceCode 待执行的Java源代码字符串
     * @param params     传递给方法的参数列表
     * @return 源代码执行后的返回值
     * @throws ClassNotFoundException 如果编译、加载类或方法调用过程中发生错误
     */
    public static <R> R executeCode(String sourceCode, Object... params) throws ClassNotFoundException {
        return executeCode(null, sourceCode, params);
    }

    /**
     * 执行给定的Java源代码并返回结果。
     *
     * @param <R>        返回值类型
     * @param sourceCode 待执行的Java源代码字符串
     * @param params     传递给方法的参数列表
     * @return 源代码执行后的返回值
     * @throws ClassNotFoundException 如果编译、加载类或方法调用过程中发生错误
     */
    public static <R> R executeCode(ClassLoader parentClassLoader, String sourceCode, Object... params) throws ClassNotFoundException {
        return executeCode(parentClassLoader, sourceCode, null, params);
    }


    /**
     * 执行给定的Java源代码并返回结果。
     *
     * @param <R>        返回值类型
     * @param sourceCode 待执行的Java源代码字符串
     * @param params     传递给方法的参数列表
     * @return 源代码执行后的返回值
     * @throws ClassNotFoundException 如果编译、加载类或方法调用过程中发生错误
     */
    public static <R> R executeCode(String sourceCode, Iterable<Object> importPackageIter, Object... params) throws ClassNotFoundException {
        return executeCode(null, sourceCode, importPackageIter, params);
    }

    /**
     * 执行给定的Java源代码并返回结果。
     *
     * @param <R>        返回值类型
     * @param sourceCode 待执行的Java源代码字符串
     * @param params     传递给方法的参数列表
     * @return 源代码执行后的返回值
     * @throws ClassNotFoundException 如果编译、加载类或方法调用过程中发生错误
     */
    public static <R> R executeCode(ClassLoader parentClassLoader, String sourceCode, Iterable<Object> importPackageIter, Object... params) throws ClassNotFoundException {
        final String className = "_" + UUID.randomUUID().toString().replace("-", "");
        final StringBuilder importPackageBuilder = new StringBuilder("import java.util.function.Function;");
        importPackageBuilder.append(System.lineSeparator());
        if (importPackageIter != null) {
            importPackageIter.forEach(importPackage -> importPackageBuilder.append("import ").append(toImportStr(importPackage)).append(";").append(System.lineSeparator()));
        }
        sourceCode = String.format(FUNCTION_CLASS_FORMAT, importPackageBuilder, className, sourceCode);
        final Function<Object[], R> function = CompilerUtil.newInstance(parentClassLoader, className, sourceCode);
        return function.apply(params);
    }

    /**
     * 将传入的对象转换为导入字符串。
     * <p>
     * 该方法根据传入对象的类型进行不同的处理：
     * - 如果是 Class 类型，则返回其完整类名；
     * - 如果是 Package 类型，则返回包名并附加 ".*"；
     * - 如果是 String 类型，则直接返回该字符串；
     * - 如果以上都不匹配，则默认返回 Object.class 的完整类名。
     *
     * @param importPackage 需要转换为导入字符串的对象，可以是 Class、Package 或 String 类型
     * @return 转换后的导入字符串
     */
    private static CharSequence toImportStr(Object importPackage) {
        // 处理 Class 类型：返回类的完整名称
        if (importPackage instanceof Class) {
            return ((Class<?>) importPackage).getName();
        }
        // 处理 Package 类型：返回包名并附加 ".*"
        if (importPackage instanceof Package) {
            return ((Package) importPackage).getName() + ".*";
        }

        // 处理 String 类型：直接返回字符串内容
        if (importPackage instanceof CharSequence) {
            return (CharSequence) importPackage;
        }
        throw new IllegalArgumentException("Unsupported import package type: " + importPackage.getClass().getName());
    }


}
