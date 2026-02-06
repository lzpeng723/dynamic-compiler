package io.github.lzpeng.compiler.resource;

import io.github.lzpeng.compiler.util.UriUtil;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 字符串资源，字符串做为资源
 *
 * @author lzpeng723
 */
public class StringResource implements Resource {

    /**
     * 表示资源的类名，可以为null。此字段用于标识资源所属的类或提供额外的上下文信息。
     * 在构造StringResource实例时指定，如果未指定则可能为null。
     * 该值可通过getName方法获取。
     */
    private final String className;
    /**
     * 字符串形式的源代码，表示资源的实际内容。
     * 该字段在构造StringResource实例时指定，并用于生成包含资源内容的输入流。
     * 源代码的内容将根据指定的字符集（默认为UTF-8）转换为字节流。
     */
    private final String sourceCode;
    /**
     * 用于指定资源内容的字符集。默认情况下，使用UTF-8编码。
     * 此字段在构造StringResource实例时指定，并用于将源代码转换为字节流。
     */
    private final Charset charset;

    /**
     * 构造一个新的StringResource实例，用于处理字符串形式的资源。
     *
     * @param sourceCode 字符串形式的源代码
     */
    public StringResource(String sourceCode) {
        this(null, sourceCode);
    }


    /**
     * 构造一个新的StringResource实例，用于处理字符串形式的资源。
     *
     * @param className  类名，可以为null
     * @param sourceCode 字符串形式的源代码
     */
    public StringResource(String className, String sourceCode) {
        this(className, sourceCode, StandardCharsets.UTF_8);
    }

    /**
     *
     * @param className  类名，可以为null
     * @param sourceCode  字符串形式的源代码
     * @param charset 编码
     */
    public StringResource(String className, String sourceCode, Charset charset) {
        this.className = className;
        this.sourceCode = sourceCode;
        this.charset = charset;
    }

    /**
     * 获取资源的名称。
     *
     * @return 资源的类名，如果未指定则可能为null
     */
    @Override
    public String getName() {
        return this.className;
    }

    @Override
    public URI getUri() {
        return UriUtil.getStringURI(this.className);
    }

    /**
     * 获取表示资源内容的输入流。
     *
     * @return 包含资源内容的{@link InputStream}对象
     */
    @Override
    public InputStream getInputStream() {
        return new BufferedInputStream(new ByteArrayInputStream(this.sourceCode.getBytes(this.charset)));
    }
}
