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

    private static final String FUNCTION_CLASS_FORMAT = "import java.util.function.Function; public final class %s implements Function<Object[], Object> { public Object apply(Object[] params) { %s } }";


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
        return create().addSource(className, sourceCode).compile();
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
     * @throws Exception 如果编译或加载过程中发生错误
     */
    public static <T> Class<T> loadClass(String className, String sourceCode) throws Exception {
        final ClassLoader classLoader = CompilerUtil.compile(className, sourceCode);
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
     * @throws Exception 如果编译、加载类或创建实例过程中发生错误
     */
    public static <T> T newInstance(String className, String sourceCode, Object... params) throws Exception {
        final Class<T> clazz = CompilerUtil.loadClass(className, sourceCode);
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
     * @throws Exception 如果编译、加载类或方法调用过程中发生错误
     */
    public static <T> T invokeStaticMethod(String className, String sourceCode, String methodName, Object... params) throws Exception {
        final Class<T> clazz = CompilerUtil.loadClass(className, sourceCode);
        final Method method = ReflectUtil.getMethod(clazz, methodName, params);
        if (Modifier.isStatic(method.getModifiers())) {
            return ReflectUtil.invokeMethod(null, method, params);
        }
        throw new IllegalArgumentException(method + " is not static");
    }


    /**
     * 动态编译给定的Java源代码并调用指定类中的实例方法。
     *
     * @param <T>          返回值类型
     * @param className    类的全限定名
     * @param sourceCode   类的Java源代码字符串
     * @param methodName   需要调用的方法名称
     * @param params       传递给方法的参数列表
     * @return 方法执行后的返回值
     * @throws Exception 如果编译、加载类、创建实例或方法调用过程中发生错误
     */
    public static <T> T invokeInstanceMethod(String className, String sourceCode, String methodName, Object... params) throws Exception {
        final Class<T> clazz = CompilerUtil.loadClass(className, sourceCode);
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
     * @throws Exception 如果编译、加载类或方法调用过程中发生错误
     */
    public static <R> R executeCode(String sourceCode, Object... params) throws Exception {
        final String className = "_" + UUID.randomUUID().toString().replace("-", "");
        sourceCode = String.format(FUNCTION_CLASS_FORMAT, className, sourceCode);
        final Function<Object[], R> function = CompilerUtil.newInstance(className, sourceCode);
        return function.apply(params);
    }


}
