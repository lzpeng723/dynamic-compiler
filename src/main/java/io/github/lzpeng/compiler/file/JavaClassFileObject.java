package io.github.lzpeng.compiler.file;


import java.io.ByteArrayOutputStream;

/**
 * 代表一个Java类字节码文件对象，用于在内存中暂存类字节码，以便可以在ClassLoader中动态加载。
 * 继承自StreamJavaFileObject，通过指定类名和输出流（默认为ByteArrayOutputStream）来初始化。
 */
public final class JavaClassFileObject extends StreamJavaFileObject {

    /**
     * 构造一个Java字节码文件对象，该对象用于在内存中暂存类字节码，以便可以在ClassLoader中动态加载。
     *
     * @param className 类名
     */
    public JavaClassFileObject(String className) {
        super(className, new ByteArrayOutputStream());
    }

}