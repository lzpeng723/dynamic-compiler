package io.github.lzpeng.compiler.file;

import io.github.lzpeng.compiler.classloader.ResourceClassLoader;
import io.github.lzpeng.compiler.resource.FileObjectResource;
import io.github.lzpeng.compiler.resource.Resource;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Java 字节码文件对象管理器
 *
 * <p>
 * 正常我们使用javac命令编译源码时会将class文件写入到磁盘中，但在运行时动态编译类不适合保存在磁盘中
 * 我们采取此对象来管理运行时动态编译类生成的字节码。
 * </p>
 *
 * @author lzpeng
 * @since 1.0.0-M1
 */
public final class JavaClassFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    /**
     * 文件缓存
     */
    private static final Map<String, File> fileCacheMap = new ConcurrentHashMap<>();

    /**
     * 存储java字节码文件对象映射
     */
    private final Map<String, Resource> classFileObjectMap = new HashMap<>();

    /**
     *
     */
    private final Set<String> loadedPackageNameSet = new HashSet<>();

    /**
     * 加载动态编译生成类的父类加载器
     */
    private final ClassLoader parentClassLoader;


    /**
     * 构造
     *
     * @param parentClassLoader 父类加载器
     * @param fileManager       字节码文件管理器
     * @param hasHttpUrl        是否有http的url
     */
    public JavaClassFileManager(ClassLoader parentClassLoader, JavaFileManager fileManager, boolean hasHttpUrl) {
        super(fileManager);
        this.parentClassLoader = Optional.ofNullable(parentClassLoader).orElseGet(Thread.currentThread()::getContextClassLoader);
    }

    /**
     * 获得动态编译生成的类的类加载器
     *
     * @param location 源码位置
     * @return 动态编译生成的类的类加载器
     */
    @Override
    public ClassLoader getClassLoader(final Location location) {
        if (StandardLocation.CLASS_OUTPUT.equals(location)) {
            return new ResourceClassLoader<>(this.parentClassLoader, this.classFileObjectMap);
        }
        return super.getClassLoader(location);
    }

    /**
     * 获得Java字节码文件对象
     * 编译器编译源码时会将Java源码对象编译转为Java字节码对象
     *
     * @param location  源码位置
     * @param className 类名
     * @param kind      文件类型
     * @param sibling   Java源码对象
     * @return Java字节码文件对象
     */
    @Override
    public JavaFileObject getJavaFileForOutput(final Location location, final String className, final Kind kind, final FileObject sibling) throws IOException {
        if (StandardLocation.CLASS_OUTPUT.equals(location) && Kind.CLASS.equals(kind)) {
            final StandardJavaFileManager standardJavaFileManager = (StandardJavaFileManager) this.fileManager;
            final Iterable<? extends File> classOutputFileIter = standardJavaFileManager.getLocation(StandardLocation.CLASS_OUTPUT);
            // Creates and returns JavaFileObject; updates classFileObjectMap
            if (classOutputFileIter != null && classOutputFileIter.iterator().hasNext()) {
                final JavaFileObject javaFileObject = super.getJavaFileForOutput(location, className, kind, sibling);
                this.classFileObjectMap.put(className, new FileObjectResource(javaFileObject));
                return javaFileObject;
            } else {
                final JavaFileObject javaFileObject = new JavaClassFileObject(className);
                this.classFileObjectMap.put(className, new FileObjectResource(javaFileObject));
                return javaFileObject;
            }
        }
        return super.getJavaFileForOutput(location, className, kind, sibling);
    }

    /**
     * 列出指定位置、包名和类型集合中的Java文件对象。
     *
     * @param location    代码的位置
     * @param packageName 包名称
     * @param kinds       文件类型集合
     * @param recurse     是否递归列出子目录
     * @return 指定条件下的Java文件对象集合
     * @throws IOException 如果发生I/O错误
     */
    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
/*
        if (this.fileManager instanceof StandardJavaFileManager && StandardLocation.CLASS_PATH.equals(location) && !this.loadedPackageNameSet.contains(packageName)) {
            final Set<File> classPathSet = new HashSet<>();
            final StandardJavaFileManager standardJavaFileManager = (StandardJavaFileManager) this.fileManager;
            final Iterable<? extends File> fileLocationIter = standardJavaFileManager.getLocation(location);
            fileLocationIter.forEach(classPathSet::add);
            final String packageRes = packageName.replace(CharPool.DOT, CharPool.SLASH);
            final Enumeration<URL> enumeration = this.parentClassLoader.getResources(packageRes);
            while (enumeration.hasMoreElements()) {
                // 获取文件
                final URL url = enumeration.nextElement();
                final File file = UrlUtil.toFile(url);
                classPathSet.add(file);
            }
            standardJavaFileManager.setLocation(location, classPathSet);
            this.loadedPackageNameSet.add(packageName);
        }
 */
        return super.list(location, packageName, kinds, recurse);
    }

}
