package io.github.lzpeng.compiler.resource;

import java.nio.file.Path;

/**
 * PathResource类扩展了URIResource类，用于根据给定的路径创建资源。
 * 该类通过接收一个Path对象作为构造参数，并将其转换为文件名和对应的URI来初始化父类。
 * 这使得可以通过Java NIO的Path接口轻松地从文件系统路径创建资源实例。
 *
 */
public class PathResource extends URIResource {

    /**
     * 构造一个新的PathResource实例，用于根据给定的路径创建资源。
     *
     * @param path 用于创建资源实例的Path对象
     */
    public PathResource(Path path) {
        super(path.toFile().getName(), path.toUri());
    }

}
