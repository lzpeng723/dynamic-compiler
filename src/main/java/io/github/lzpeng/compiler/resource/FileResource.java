package io.github.lzpeng.compiler.resource;

import java.io.File;

/**
 * FileResource类扩展了URIResource类，提供了一种通过Java的File对象来创建资源实例的方式。
 * 它接收一个File对象作为构造参数，并使用该对象生成对应的文件名和URI以初始化其父类。
 * 该类使得可以方便地从本地文件系统中的文件创建资源表示。
 *
 * @see URIResource
 */
public class FileResource extends URIResource {

    /**
     * 构造一个新的FileResource实例，用于表示给定文件的资源。
     *
     * @param file 用于创建资源的File对象
     */
    public FileResource(File file) {
        super(file.getName(), file.toURI());
    }

}
