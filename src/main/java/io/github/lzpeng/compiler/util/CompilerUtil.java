package io.github.lzpeng.compiler.util;

import io.github.lzpeng.compiler.JavaSourceCompiler;

import java.io.File;

/**
 * 源码编译工具类，主要封装{@link JavaSourceCompiler} 相关功能
 *
 * @author lzpeng723
 * @since 1.0.0-M1
 */
public final class CompilerUtil {

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

}
