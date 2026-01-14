package io.github.lzpeng.compiler;

import io.github.lzpeng.compiler.file.JavaClassFileManager;
import io.github.lzpeng.compiler.file.JavaSourceFileObject;
import io.github.lzpeng.compiler.resource.FileResource;
import io.github.lzpeng.compiler.resource.PathResource;
import io.github.lzpeng.compiler.resource.Resource;
import io.github.lzpeng.compiler.resource.StringResource;
import io.github.lzpeng.compiler.util.UrlUtil;

import javax.annotation.processing.Processor;
import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Java 源码编译器
 *
 * @author lzpeng
 */
public final class JavaSourceCompiler {

    /**
     * 系统默认的Java编译器实例。通过ToolProvider.getSystemJavaCompiler()方法获取，用于执行Java源代码的编译任务。
     * 该变量为静态常量，确保在类加载时初始化，并且在整个应用程序生命周期中保持不变。
     */
    private final JavaCompiler systemCompiler = ToolProvider.getSystemJavaCompiler();


    /**
     * 标准Java文件管理器实例，用于管理和访问编译过程中所需的源代码和类文件。
     * 该实例通过系统编译器获取，并配置为不使用任何特定的诊断监听器或位置。
     */
    private final StandardJavaFileManager standardFileManager = this.systemCompiler.getStandardFileManager(null, null, null);

    /**
     * 待编译的资源，支持：
     *
     * <ul>
     *     <li>源码字符串，使用{@link StringResource}</li>
     *     <li>源码文件、源码jar包或源码zip包，亦或者文件夹，使用{@link FileResource}</li>
     * </ul>
     * 可以是 .java文件 压缩文件 文件夹 递归搜索文件夹内的zip包和jar包
     */
    private final List<Resource> sourceList = new ArrayList<>();


    /**
     * 存储编译过程中使用的处理器列表。这些处理器可以用于处理注解、生成额外的源代码等。
     * 该列表支持添加多种类型的处理器，如文件、URL或直接指定的处理器对象。
     * 处理器列表中的元素类型为{@link Processor}，代表了在编译Java源码时将要应用的各种处理器。
     * <p>
     * 通过调用相关方法可以向此列表中添加新的处理器，从而扩展编译器的功能。
     * 注意，对于需要特定类路径访问的处理器，可能还需要使用其他方法来配置类加载器或依赖项。
     */
    private final List<Processor> processorList = new ArrayList<>();

    /**
     * 用于存储与Java文件管理器位置相关的URL集合的映射。每个键是JavaFileManager.Location的一个实例，表示特定的文件管理器位置，而值是一个URL集合，这些URL指向该位置下的资源。
     * <p>
     * 此映射主要用于在编译过程中提供对不同位置上所需资源（如类路径、源代码路径等）的访问。
     */
    private final Map<JavaFileManager.Location, Collection<URL>> locationMap = new HashMap<>();

    /**
     * 编译类时使用的父类加载器
     */
    private final ClassLoader parentClassLoader;

    /**
     * 构造
     *
     * @param parent 父类加载器，null则使用默认类加载器
     */
    private JavaSourceCompiler(ClassLoader parent) {
        this.parentClassLoader = Optional.ofNullable(parent).orElseGet(Thread.currentThread()::getContextClassLoader);
    }

    /**
     * 创建Java源码编译器
     *
     * @return Java源码编译器
     */
    public static JavaSourceCompiler create() {
        return create(null);
    }

    /**
     * 创建Java源码编译器
     *
     * @param parent 父类加载器
     * @return Java源码编译器
     */
    public static JavaSourceCompiler create(ClassLoader parent) {
        return new JavaSourceCompiler(parent);
    }

    /**
     * 从给定的路径获取Java文件对象。
     *
     * @param file 源代码所在的路径
     * @return 包含所有Java文件对象的迭代器
     * @throws RuntimeException 如果在遍历路径或获取文件对象时发生IO异常
     */
    private Iterable<? extends JavaFileObject> getJavaFileObjectsFromFile(File file) {
        return getJavaFileObjectsFromPath(file.toPath());
    }

    /**
     * 从给定的路径获取Java文件对象。
     *
     * @param path 源代码所在的路径
     * @return 包含所有Java文件对象的迭代器
     * @throws RuntimeException 如果在遍历路径或获取文件对象时发生IO异常
     */
    private Iterable<? extends JavaFileObject> getJavaFileObjectsFromPath(Path path) {
        try {
            final File[] files = Files.walk(path).map(Path::toFile).filter(file -> file.isFile() && file.getName().endsWith(JavaFileObject.Kind.SOURCE.extension)).toArray(File[]::new);
            return this.standardFileManager.getJavaFileObjects(files);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 向编译器中加入待编译的源码Map
     *
     * @param sourceCodeMap 源码Map key: 类名 value 源码
     * @return Java源码编译器
     */
    public JavaSourceCompiler addSource(Map<String, String> sourceCodeMap) {
        if (sourceCodeMap != null && !sourceCodeMap.isEmpty()) {
            sourceCodeMap.forEach(this::addSource);
        }
        return this;
    }

    /**
     * 向编译器中加入待编译的源码
     *
     * @param className  类名
     * @param sourceCode 源码
     * @return Java文件编译器
     */
    public JavaSourceCompiler addSource(String className, String sourceCode) {
        if (className != null && sourceCode != null) {
            this.sourceList.add(new StringResource(className, sourceCode));
        }
        return this;
    }

    /**
     * 向编译器中加入待编译的资源<br>
     * 支持 .java, 文件夹, 压缩文件 递归搜索文件夹内的压缩文件和jar包
     *
     * @param resources 待编译的资源，支持 .java, 文件夹, 压缩文件 递归搜索文件夹内的压缩文件和jar包
     * @return Java源码编译器
     */
    public JavaSourceCompiler addSource(Resource... resources) {
        if (resources != null && resources.length > 0) {
            this.sourceList.addAll(Arrays.asList(resources));
        }
        return this;
    }

    /**
     * 向编译器中加入待编译的文件<br>
     * 支持 .java, 文件夹, 压缩文件 递归搜索文件夹内的压缩文件和jar包
     *
     * @param files 待编译的文件 支持 .java, 文件夹, 压缩文件 递归搜索文件夹内的压缩文件和jar包
     * @return Java源码编译器
     */
    public JavaSourceCompiler addSource(File... files) {
        final Resource[] resources = Arrays.stream(files).map(FileResource::new).toArray(Resource[]::new);
        this.addSource(resources);
        return this;
    }

    /**
     * 加入编译Java源码时所需要的jar包，jar包中必须为字节码
     *
     * @param file 编译Java源码时所需要的jar包
     * @return Java源码编译器
     */
    public JavaSourceCompiler setSourceOutput(File file) {
        this.addLocationUrl(StandardLocation.SOURCE_OUTPUT, file);
        return this;
    }

    /**
     * 加入编译Java源码时所需要的jar包，jar包中必须为字节码
     *
     * @param file 编译Java源码时所需要的jar包
     * @return Java源码编译器
     */
    public JavaSourceCompiler setClassOutput(File file) {
        this.addLocationUrl(StandardLocation.CLASS_OUTPUT, file);
        return this;
    }

    /**
     * 加入编译Java源码时所需要的jar包，jar包中必须为字节码
     *
     * @param files 编译Java源码时所需要的jar包
     * @return Java源码编译器
     */
    public JavaSourceCompiler addSourceDirectory(File... files) {
        this.addLocationUrl(StandardLocation.SOURCE_PATH, files);
        return this;
    }

    /**
     * 加入编译Java源码时所需要的jar包，jar包中必须为字节码
     *
     * @param dependencies 编译Java源码时所需要的jar包
     * @return Java源码编译器
     */
    public JavaSourceCompiler addSourceDirectory(String... dependencies) {
        this.addLocationUrl(StandardLocation.SOURCE_PATH, dependencies);
        return this;
    }

    /**
     * 加入编译Java源码时所需要的jar包，jar包中必须为字节码
     *
     * @param urls 编译Java源码时所需要的jar包
     * @return Java源码编译器
     */
    public JavaSourceCompiler addSourceDirectory(URL... urls) {
        this.addLocationUrl(StandardLocation.SOURCE_PATH, urls);
        return this;
    }

    /**
     * 加入编译Java源码时所需要的jar包，jar包中必须为字节码
     *
     * @param files 编译Java源码时所需要的jar包
     * @return Java源码编译器
     */
    public JavaSourceCompiler addDependencyPath(File... files) {
        this.addLocationUrl(StandardLocation.CLASS_PATH, files);
        return this;
    }

    /**
     * 加入编译Java源码时所需要的jar包，jar包中必须为字节码
     *
     * @param dependencies 编译Java源码时所需要的jar包
     * @return Java源码编译器
     */
    public JavaSourceCompiler addDependencyPath(String... dependencies) {
        this.addLocationUrl(StandardLocation.CLASS_PATH, dependencies);
        return this;
    }

    /**
     * 加入编译Java源码时所需要的jar包，jar包中必须为字节码
     *
     * @param urls 编译Java源码时所需要的jar包
     * @return Java源码编译器
     */
    public JavaSourceCompiler addDependencyPath(URL... urls) {
        this.addLocationUrl(StandardLocation.CLASS_PATH, urls);
        return this;
    }

    /**
     * 向编译器中添加处理器文件。
     *
     * @param files 待添加的处理器文件数组
     * @return 当前Java源码编译器实例，用于链式调用
     */
    public JavaSourceCompiler addProcessorPath(File... files) {
        addProcessorPath(false, files);
        return this;
    }

    /**
     * 向编译器中添加处理器文件。
     *
     * @param addDependency 是否同时添加到编译路径
     * @param files         待添加的处理器文件数组
     * @return 当前Java源码编译器实例，用于链式调用
     */
    public JavaSourceCompiler addProcessorPath(boolean addDependency, File... files) {
        this.addLocationUrl(StandardLocation.ANNOTATION_PROCESSOR_PATH, files);
        if (addDependency) {
            this.addDependencyPath(files);
        }
        return this;
    }

    /**
     * 向编译器中添加处理器文件。
     *
     * @param urlStrs 待添加的处理器文件数组
     * @return 当前Java源码编译器实例，用于链式调用
     */
    public JavaSourceCompiler addProcessorPath(String... urlStrs) {
        addProcessorPath(false, urlStrs);
        return this;
    }

    /**
     * 向编译器中添加处理器文件。
     *
     * @param addDependency 是否同时添加到编译路径
     * @param urlStrs       待添加的处理器文件数组
     * @return 当前Java源码编译器实例，用于链式调用
     */
    public JavaSourceCompiler addProcessorPath(boolean addDependency, String... urlStrs) {
        this.addLocationUrl(StandardLocation.ANNOTATION_PROCESSOR_PATH, urlStrs);
        if (addDependency) {
            this.addDependencyPath(urlStrs);
        }
        return this;
    }

    /**
     * 向编译器中添加处理器文件。
     *
     * @param urls 待添加的处理器文件数组
     * @return 当前Java源码编译器实例，用于链式调用
     */
    public JavaSourceCompiler addProcessorPath(URL... urls) {
        addProcessorPath(false, urls);
        return this;
    }

    /**
     * 向编译器中添加处理器文件。
     *
     * @param addDependency 是否同时添加到编译路径
     * @param urls          待添加的处理器文件数组
     * @return 当前Java源码编译器实例，用于链式调用
     */
    public JavaSourceCompiler addProcessorPath(boolean addDependency, URL... urls) {
        this.addLocationUrl(StandardLocation.ANNOTATION_PROCESSOR_PATH, urls);
        if (addDependency) {
            this.addDependencyPath(urls);
        }
        return this;
    }

    /**
     * 向指定的Java文件管理器位置添加URL。
     *
     * @param location Java文件管理器中的位置
     * @param files    要添加到指定位置的URL数组
     * @return 当前Java源码编译器实例，用于链式调用
     */
    public JavaSourceCompiler addLocationUrl(JavaFileManager.Location location, File... files) {
        final URL[] urls = Arrays.stream(files).map(UrlUtil::getURL).toArray(URL[]::new);
        this.addLocationUrl(location, urls);
        return this;
    }

    /**
     * 向指定的Java文件管理器位置添加URL。
     *
     * @param location Java文件管理器中的位置
     * @param urlStrs  要添加到指定位置的URL数组
     * @return 当前Java源码编译器实例，用于链式调用
     */
    public JavaSourceCompiler addLocationUrl(JavaFileManager.Location location, String... urlStrs) {
        final URL[] urls = Arrays.stream(urlStrs).map(UrlUtil::getURL).toArray(URL[]::new);
        this.addLocationUrl(location, urls);
        return this;
    }

    /**
     * 向指定的Java文件管理器位置添加URL。
     *
     * @param location Java文件管理器中的位置
     * @param urls     要添加到指定位置的URL数组
     * @return 当前Java源码编译器实例，用于链式调用
     */
    public JavaSourceCompiler addLocationUrl(JavaFileManager.Location location, URL... urls) {
        if (urls != null && urls.length > 0) {
            final Collection<URL> locationUrlColl = this.locationMap.computeIfAbsent(location, __ -> new ArrayList<>());
            locationUrlColl.addAll(Arrays.asList(urls));
        }
        return this;
    }


    /**
     * 向编译器中添加注解处理器。
     *
     * @param processor 待添加的注解处理器
     * @return 当前Java源码编译器实例，用于链式调用
     */
    public JavaSourceCompiler addProcessor(Processor processor) {
        this.processorList.add(processor);
        return this;
    }

    /**
     * 从编译器中移除类路径。
     *
     * @return 当前Java源码编译器实例，用于链式调用
     */
    public JavaSourceCompiler removeClassPath() {
        return this.removeLocation(StandardLocation.CLASS_PATH);
    }

    /**
     * 从编译器中移除注解处理器路径。
     *
     * @return 当前Java源码编译器实例，用于链式调用
     */
    public JavaSourceCompiler removeProcessor() {
        return this.removeLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH);
    }

    /**
     * 从编译器中移除类输出位置。
     *
     * @return 当前Java源码编译器实例，用于链式调用
     */
    public JavaSourceCompiler removeClassOutput() {
        return this.removeLocation(StandardLocation.CLASS_OUTPUT);
    }

    /**
     * 从编译器中移除源输出位置。
     *
     * @return 当前Java源码编译器实例，用于链式调用
     */
    public JavaSourceCompiler removeSourceOutput() {
        return this.removeLocation(StandardLocation.SOURCE_OUTPUT);
    }

    /**
     * 从编译器中移除指定的Java文件管理器位置。
     *
     * @param location 要移除的位置
     * @return 当前Java源码编译器实例，用于链式调用
     */
    public JavaSourceCompiler removeLocation(JavaFileManager.Location location) {
        this.locationMap.remove(location);
        return this;
    }

    /**
     * 清除编译器中的所有位置映射。
     *
     * @return 当前Java源码编译器实例，用于链式调用
     */
    public JavaSourceCompiler clear() {
        this.locationMap.clear();
        return this;
    }

    /**
     * 编译所有文件并返回类加载器
     *
     * @return 类加载器
     */
    public ClassLoader compile() {
        return compile(null);
    }

    /**
     * 编译所有文件并返回类加载器
     *
     * @param options 编译参数
     * @return 类加载器
     */
    public ClassLoader compile(Iterable<String> options) {
        if (sourceList.isEmpty() && this.locationMap.isEmpty()) {
            // 没有需要编译的源码文件返回加载zip或jar包的类加载器
            return this.parentClassLoader;
        }
        // 获得classPath
        final Collection<URL> classPathColl = this.locationMap.getOrDefault(StandardLocation.CLASS_PATH, Collections.emptySet());
        final ClassLoader classLoader = classPathColl.isEmpty() ? this.parentClassLoader : URLClassLoader.newInstance(classPathColl.toArray(new URL[0]), this.parentClassLoader);
        // 创建编译器
        try (final JavaClassFileManager javaFileManager = new JavaClassFileManager(classLoader, this.standardFileManager)) {
            // classpath
            if (null == options) {
                options = new ArrayList<>();
            }
            // 设置编译时候用到的路径
            this.locationMap.forEach((location, urls) -> {
                try {
                    if (urls != null) {
                        this.standardFileManager.setLocation(location, urls.stream().map(UrlUtil::toFile).collect(Collectors.toList()));
                    } else {
                        this.standardFileManager.setLocation(location, null);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            // 编译文件
            final DiagnosticCollector<? super JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
            final List<JavaFileObject> javaFileObjectList = this.getJavaFileObjectList();
            final CompilationTask compilerTask = this.systemCompiler.getTask(null, javaFileManager, diagnosticCollector, options, null, javaFileObjectList);
            if (!this.processorList.isEmpty()) {
                compilerTask.setProcessors(this.processorList);
            }
            if (compilerTask.call()) {
                // 加载编译后的类
                return javaFileManager.getClassLoader(StandardLocation.CLASS_OUTPUT);
            }
            //编译失败,收集错误信息
            throw new CompilerException(this.getMessages(diagnosticCollector));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取Java文件对象列表。
     * <p>
     * 该方法遍历已添加的资源列表，根据资源类型生成相应的Java文件对象。对于文件资源，它会递归地获取文件路径下的所有文件，并将这些路径转换为Java文件对象。对于其他类型的资源，则直接创建对应的Java文件对象。
     *
     * @return 包含所有Java文件对象的列表
     */
    private List<JavaFileObject> getJavaFileObjectList() {
        final List<JavaFileObject> list = new ArrayList<>();
        final Iterable<? extends File> sourceFileLocation = this.standardFileManager.getLocation(StandardLocation.SOURCE_PATH);
        if (sourceFileLocation != null) {
            final Stream<? extends File> stream = StreamSupport.stream(sourceFileLocation.spliterator(), false);
            stream.forEach(file -> getJavaFileObjectsFromFile(file).forEach(list::add));
        }
        for (Resource resource : this.sourceList) {
            if (resource instanceof FileResource || resource instanceof PathResource) {
                final Path path = resource.getPath();
                getJavaFileObjectsFromPath(path).forEach(list::add);
            } else {
                list.add(new JavaSourceFileObject(resource.getName(), resource.getInputStream()));
            }
        }

        return list;
    }

    /**
     * 获取{@link DiagnosticCollector}收集到的诊断信息，以文本返回
     *
     * @param collector {@link DiagnosticCollector}
     * @return 诊断消息
     */
    private <T> String getMessages(DiagnosticCollector<T> collector) {
        final List<Diagnostic<? extends T>> diagnostics = collector.getDiagnostics();
        return diagnostics.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(System.lineSeparator()));
    }

}
