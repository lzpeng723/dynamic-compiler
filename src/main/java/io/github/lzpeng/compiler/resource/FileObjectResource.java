package io.github.lzpeng.compiler.resource;

import javax.tools.FileObject;
import java.io.IOException;
import java.io.InputStream;

/**
 * FileObjectResource类扩展了URIResource类，提供了一种通过FileObject对象来创建资源实例的方式。
 * 它接收一个FileObject对象作为构造参数，并使用该对象生成对应的文件名和URI以初始化其父类。
 * 该类使得可以从支持FileObject接口的各种文件系统或存储中创建资源表示。
 *
 * @see URIResource
 */
public class FileObjectResource extends URIResource {

    /**
     * 表示用于创建资源的FileObject对象。
     * 该字段在构造FileObjectResource实例时指定，并通过它来获取资源的相关信息和输入流。
     * FileObject提供了对文件或目录的操作，包括但不限于读取内容、获取元数据等。
     * 该字段为final类型，意味着一旦被初始化后便不可更改。
     */
    private final FileObject fileObject;

    /**
     * 构造一个新的FileObjectResource实例。
     *
     * @param fileObject 用于创建资源的FileObject对象
     */
    public FileObjectResource(FileObject fileObject) {
        super(fileObject.getName(), fileObject.toUri());
        this.fileObject = fileObject;
    }


    /**
     * 获取此资源的输入流。
     *
     * @return 返回表示该资源内容的{@link InputStream}对象。如果在获取流时发生I/O错误，则抛出运行时异常。
     */
    @Override
    public InputStream getInputStream() {
        try {
            return this.fileObject.openInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
