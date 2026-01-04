package io.github.lzpeng.compiler.file;


import java.io.InputStream;

/**
 * 代表一个Java源码文件对象，该对象通过提供的名称和输入流来表示一个Java源文件。
 * 继承自 {@link StreamJavaFileObject}，用于在内存中处理Java源代码。
 */
public final class JavaSourceFileObject extends StreamJavaFileObject {

    /**
     * 构造一个Java源码文件对象，该对象可以通过提供的名称和输入流来表示一个Java源文件。
     *
     * @param name        文件名，不包括扩展名。
     * @param inputStream 与此文件对象关联的输入流，用于读取数据。
     */
    public JavaSourceFileObject(String name, InputStream inputStream) {
        super(name, inputStream);
    }


}
