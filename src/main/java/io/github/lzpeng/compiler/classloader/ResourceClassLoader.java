package io.github.lzpeng.compiler.classloader;

import io.github.lzpeng.compiler.CharPool;
import io.github.lzpeng.compiler.resource.Resource;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.*;

/**
 * 资源类加载器，可以加载任意类型的资源类
 *
 * @param <T> {@link Resource}接口实现类
 * @author lzpeng723
 */
public final class ResourceClassLoader<T extends Resource> extends SecureClassLoader {

    /**
     * 存储资源名称到资源对象的映射。键为资源名称，值为对应的资源对象。
     * 该映射用于在类加载过程中根据类名查找相应的资源，并从中读取类定义所需的二进制数据。
     */
    private final Map<String, T> resourceMap;
    /**
     * 缓存已经加载的类
     */
    private final Map<String, Class<?>> cacheClassMap;

    /**
     * 构造
     *
     * @param parentClassLoader 父类加载器，null表示默认当前上下文加载器
     * @param resourceMap       资源map
     */
    public ResourceClassLoader(final ClassLoader parentClassLoader, final Map<String, T> resourceMap) {
        super(Optional.ofNullable(parentClassLoader).orElseGet(ResourceClassLoader.class::getClassLoader));
        this.resourceMap = Optional.ofNullable(resourceMap).orElseGet(HashMap::new);
        this.cacheClassMap = new HashMap<>();
    }

    /**
     * 增加需要加载的类资源
     *
     * @param resource 资源，可以是文件、流或者字符串
     * @return this
     */
    public ResourceClassLoader<T> addResource(final T resource) {
        this.resourceMap.put(resource.getName(), resource);
        return this;
    }

    /**
     * 根据给定的类名查找并定义类。
     * 首先尝试从缓存中获取已加载的类。如果缓存中不存在，则通过实现的{@code defineByName}方法定义类。
     * 如果仍然无法找到或定义该类，则调用父类的findClass方法继续寻找。
     *
     * @param name 要查找和定义的类的全限定名
     * @return 找到或新定义的类对象
     * @throws ClassNotFoundException 如果找不到指定的类
     */
    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            final Class<?> clazz = cacheClassMap.computeIfAbsent(name, this::defineByName);
            if (clazz == null) {
                return super.findClass(name);
            }
            return clazz;
        }
    }

    @Override
    protected URL findResource(String name) {
        if (name.endsWith(JavaFileObject.Kind.CLASS.name())) {
            name = name.substring(0, name.length() - JavaFileObject.Kind.CLASS.name().length());
        }
        T resource = this.resourceMap.get(name);
        if (resource != null) {
            return resource.getUrl();
        }
        resource = this.resourceMap.get(name.replace(CharPool.DOT, CharPool.SLASH));
        if (resource != null) {
            return resource.getUrl();
        }
        return super.findResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        final URL resource = this.findResource(name);

        return new Enumeration<URL>() {

            private URL url = resource;

            @Override
            public boolean hasMoreElements() {
                return Objects.nonNull(this.url);
            }

            @Override
            public URL nextElement() {
                if (!hasMoreElements()) {
                    throw new NoSuchElementException();
                }
                final URL next = this.url;
                this.url = null;
                return next;
            }
        };
    }

    /**
     * 从给定资源中读取class的二进制流，然后生成类<br>
     * 如果这个类资源不存在，返回{@code null}
     *
     * @param name 类名
     * @return 定义的类
     */
    private Class<?> defineByName(final String name) {
        final Resource resource = resourceMap.get(name);
        if (null != resource) {
            final byte[] bytes = resource.readBytes();
            resource.getUri();
            try {
                final ProtectionDomain protectionDomain = new ProtectionDomain(new CodeSource(resource.getUrl(), (Certificate[]) null), null, this, null);
                return defineClass(name, bytes, 0, bytes.length, protectionDomain);
            } catch (Throwable t) {
                return defineClass(name, bytes, 0, bytes.length);
            }
        }
        return null;
    }
}
