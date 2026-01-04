package io.github.lzpeng.compiler.resource;

/**
 * ClassPathResource类扩展了URLResource类，专门用于从Java类路径中加载资源。
 * 它提供了构造函数来根据指定的路径或名称和路径创建资源实例，并使用默认或指定的类加载器来定位资源。
 * 该类继承自URLResource，因此它也具备了通过URL访问资源的能力。
 */
public class ClassPathResource extends URLResource {

    /**
     * 构造一个新的ClassPathResource实例，用于从类路径中加载资源。
     *
     * @param path 资源的路径
     */
    public ClassPathResource(String path) {
        this(null, path);
    }


    /**
     * 构造一个新的ClassPathResource实例，用于从类路径中加载资源。
     *
     * @param name 资源的名称
     * @param path 资源在类路径中的位置
     */
    public ClassPathResource(String name, String path) {
        this(name, path, ClassPathResource.class.getClassLoader());
    }

    /**
     * 构造一个新的ClassPathResource实例，用于从类路径中加载资源。
     *
     * @param name        资源的名称
     * @param path        资源在类路径中的位置
     * @param classLoader 资源类加载器
     */
    public ClassPathResource(String name, String path, ClassLoader classLoader) {
        super(name, classLoader.getResource(path));
    }


}
