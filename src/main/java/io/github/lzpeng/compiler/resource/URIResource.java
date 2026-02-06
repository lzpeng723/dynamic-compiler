package io.github.lzpeng.compiler.resource;


import java.net.URI;

/**
 * URIResource类实现了Resource接口，用于表示具有统一资源标识符（URI）的资源。
 * 该类提供了获取资源名称和URI的方法。通过构造函数接收资源名称和URI作为参数，
 * 并将它们存储在不可变的私有字段中。这些字段确保了资源一旦创建后其名称和URI不会被修改。
 */
public class URIResource implements Resource {
    /**
     * 表示资源的名称。该字段在构造URIResource实例时指定，并通过getName方法提供对资源名称的访问。
     * 资源名称可以是文件名、路径或其他标识符，具体取决于创建资源实例的方式。
     * 该字段为final类型，意味着一旦被初始化后便不可更改。
     */
    private final String name;

    /**
     * 表示资源的统一资源标识符（URI）。该字段在构造URIResource实例时指定，并通过getUri方法提供对资源URI的访问。
     * 资源URI用于唯一地标识资源的位置，可以是文件路径、网络位置或其他形式的标识符。
     * 该字段为final类型，意味着一旦被初始化后便不可更改。
     */
    private final URI uri;

    /**
     *
     * @param name 资源名称
     * @param uri  资源uri
     */
    public URIResource(String name, URI uri) {
        this.name = name;
        this.uri = uri;
    }

    /**
     * 获取资源的名称。
     *
     * @return 资源的名称
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * 获取资源的统一资源标识符（URI）。
     *
     * @return 资源的URI，用于唯一地标识资源的位置
     */
    @Override
    public URI getUri() {
        return this.uri;
    }

}
