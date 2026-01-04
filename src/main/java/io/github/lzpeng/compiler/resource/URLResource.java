package io.github.lzpeng.compiler.resource;

import io.github.lzpeng.compiler.util.UriUtil;

import java.net.URL;

/**
 * URLResource类扩展了URIResource类，提供了通过URL来创建资源的功能。
 * 它接收一个资源名称和一个URL对象作为构造参数，并使用该URL对象生成对应的URI，
 * 以初始化其父类。如果提供的URL格式不正确，则抛出IllegalArgumentException异常。
 */
public class URLResource extends URIResource {


    /**
     * 构造一个新的URLResource实例。
     *
     * @param name 资源的名称
     * @param url  用于创建资源的URL对象
     * @throws IllegalArgumentException 如果提供的URL格式错误
     */
    public URLResource(String name, URL url) {
        super(name, UriUtil.getURI(url));
    }

}
