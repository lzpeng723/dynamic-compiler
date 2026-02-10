package io.github.lzpeng.compiler;

import io.github.lzpeng.compiler.file.JavaClassFileManager;
import io.github.lzpeng.compiler.file.JavaSourceFileObject;
import io.github.lzpeng.compiler.resource.FileResource;
import io.github.lzpeng.compiler.resource.PathResource;
import io.github.lzpeng.compiler.resource.Resource;
import io.github.lzpeng.compiler.resource.StringResource;
import io.github.lzpeng.compiler.util.JarFileUtil;
import io.github.lzpeng.compiler.util.JdkVersionUtil;
import io.github.lzpeng.compiler.util.ReflectUtil;
import io.github.lzpeng.compiler.util.UrlUtil;

import javax.annotation.processing.Processor;
import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
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
     * 运行时信息，用于获取进程信息
     */
    private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

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
     * 是否有 httpUrl
     */
    private boolean hasHttpUrl;

    /**
     * 构造
     *
     * @param parent 父类加载器，null则使用默认类加载器
     */
    private JavaSourceCompiler(ClassLoader parent) {
        try {
            this.standardFileManager.setLocation(StandardLocation.CLASS_PATH, new ArrayList<>());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.parentClassLoader = Optional.ofNullable(parent).orElseGet(Thread.currentThread()::getContextClassLoader);
        this.addDependencyPath(this.parentClassLoader);
        this.withClassPath();
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
     * 配置Java源码编译器的类路径依赖。
     * 该方法会从运行时环境获取类路径，并将其解析为文件路径添加到依赖中。
     * 同时，还会加载当前类加载器中的所有资源路径作为依赖。
     *
     * @return 返回当前JavaSourceCompiler实例，支持链式调用。
     * @throws RuntimeException 如果在加载资源路径时发生IO异常，则抛出运行时异常。
     */
    public JavaSourceCompiler withClassPath() {
        // 获取运行时类路径字符串并按分隔符拆分为多个路径
        final String classPathStr = runtimeMXBean.getClassPath();
        final String[] classPathStrs = classPathStr.split(File.pathSeparator);
        Arrays.stream(classPathStrs).map(File::new).forEach(this::addDependencyPath);
        return this;
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
            final Collection<URL> locationUrlColl = this.locationMap.computeIfAbsent(location, __ -> new LinkedHashSet<>());
            for (URL url : urls) {
                if (url.getProtocol().startsWith("http")) {
                    this.hasHttpUrl = true;
                }
                locationUrlColl.add(url);
            }
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
        this.sourceList.clear();
        this.processorList.clear();
        this.locationMap.clear();
        return this;
    }

    /**
     * 编译所有文件并返回类加载器
     *
     * @return 类加载器
     */
    public ClassLoader compile() {
        return this.compile(null);
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
        try (final JavaClassFileManager javaFileManager = new JavaClassFileManager(classLoader, this.standardFileManager, this.hasHttpUrl)) {
            // 初始化编译参数
            if (options == null) {
                options = Collections.emptySet();
            }
            // 设置 Location
            this.setLocation();
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
            if (JdkVersionUtil.isJdkMinus(8)) {
                final List<Diagnostic<? extends JavaFileObject>> diagnosticList = ReflectUtil.getFieldValue(diagnosticCollector, "diagnostics");
                final Map<File, Object> archiveMap = ReflectUtil.getFieldValue(this.standardFileManager, "archives");
                archiveMap.forEach((file, archive) -> {
                    if (file.isFile()) {
                        if (archive.getClass().getName().equals("com.sun.tools.javac.file.JavacFileManager$MissingArchive")) {
                            diagnosticList.add(0, new UrlCacheDiagnostic<>(file, archive));
                        }
                    }
                });
            }
            //编译失败,收集错误信息
            throw new CompilerException(this.getMessages(diagnosticCollector));
        } catch (IOException e) {
            throw new CompilerException(e.getMessage(), e);
        }
    }


    /**
     * 设置编译时所需的类路径和其他资源定位信息。
     * 该方法遍历locationMap中的每个location，将其对应的文件路径设置到standardFileManager中。
     * 如果某个location对应的urls为null，则将其路径设置为null。
     * 在处理过程中，如果发生IO异常，则抛出运行时异常。
     */
    private void setLocation() {
        // 遍历locationMap，为每个location设置对应的文件路径
        // 这些路径用于编译时的类路径和其他资源定位
        this.locationMap.forEach((location, urls) -> {
            try {
                if (urls != null) {
                    // 将URL转换为文件，并通过JarFileUtil处理后设置到standardFileManager中
                    this.standardFileManager.setLocation(location, urls.stream().map(UrlUtil::toFile).map(JarFileUtil::list).flatMap(Collection::stream).collect(Collectors.toSet()));
                    //this.standardFileManager.setLocation(location, urls.stream().map(UrlUtil::toFile).collect(Collectors.toSet()));
                } else {
                    // 如果urls为null，则将该location的路径设置为null
                    this.standardFileManager.setLocation(location, null);
                }
            } catch (IOException e) {
                // 捕获IO异常并抛出运行时异常
                throw new RuntimeException(e);
            }
        });
    }


    /**
     * 递归遍历给定的类加载器及其父类加载器，收集所有URLClassLoader中的依赖路径。
     *
     * @param classLoader 要处理的类加载器实例。如果为null，则直接返回。
     *                    该方法会递归处理其父类加载器，直到根类加载器为止。
     */
    private void addDependencyPath(ClassLoader classLoader) {
        // 如果传入的类加载器为null，直接返回，无需处理
        if (classLoader == null) {
            return;
        }
        // 加载当前类加载器中的所有资源路径并添加为依赖
        try {
            final Enumeration<URL> enumeration = classLoader.getResources("");
            while (enumeration.hasMoreElements()) {
                final URL url = enumeration.nextElement();
                this.addDependencyPath(url);
            }
        } catch (IOException ignore) {
        }
        // 循环遍历当前类加载器及其所有父类加载器
        do {
            // 如果当前类加载器是URLClassLoader类型，则获取其URL路径并添加到依赖路径中
            if (classLoader instanceof URLClassLoader) {
                this.addDependencyPath(((URLClassLoader) classLoader).getURLs());
            }
            // 获取当前类加载器的父类加载器，继续向上遍历
            classLoader = classLoader.getParent();
        } while (classLoader != null); // 当父类加载器为null时结束循环
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

    /**
     * UrlCacheDiagnostic 是一个用于诊断URL缓存相关问题的内部类。
     * 它实现了 Diagnostic 接口，提供了关于文件、归档对象和URL的信息，
     * 并在加载URL失败时生成相应的错误消息。
     * jdk8中 com.sun.tools.javac.file.JavacFileManager.openArchive(java.io.File, boolean) 会报错
     *
     * @param <S> 泛型参数，表示诊断信息的源类型（在此实现中未使用）。
     */
    private static class UrlCacheDiagnostic<S> implements Diagnostic<S> {

        /**
         * 文件对象，表示与URL关联的本地文件
         */
        private final File file;

        /**
         * 归档对象，可能一个压缩包或其他资源容器
         */
        private final Object archive;

        /**
         * URL对象，通过文件缓存获取的URL
         */
        private final URL url;

        /**
         * 构造函数，初始化UrlCacheDiagnostic实例。
         *
         * @param file    与URL关联的本地文件
         * @param archive 可能是压缩包或其他资源容器的对象
         */
        private UrlCacheDiagnostic(File file, Object archive) {
            this.file = file;
            this.archive = archive;
            this.url = UrlUtil.getUrlFromFileCache(file);
        }

        /**
         * 返回诊断的种类。此处固定返回 Kind.OTHER。
         *
         * @return 诊断种类，固定为 Kind.OTHER
         */
        @Override
        public Kind getKind() {
            return Kind.OTHER;
        }

        /**
         * 返回诊断信息的源。在此实现中始终返回 null。
         *
         * @return 诊断信息的源，固定为 null
         */
        @Override
        public S getSource() {
            return null;
        }

        /**
         * 返回诊断信息的位置。在此实现中始终返回 0。
         *
         * @return 位置信息，固定为 0
         */
        @Override
        public long getPosition() {
            return 0;
        }

        /**
         * 返回诊断信息的起始位置。在此实现中始终返回 0。
         *
         * @return 起始位置信息，固定为 0
         */
        @Override
        public long getStartPosition() {
            return 0;
        }

        /**
         * 返回诊断信息的结束位置。在此实现中始终返回 0。
         *
         * @return 结束位置信息，固定为 0
         */
        @Override
        public long getEndPosition() {
            return 0;
        }

        /**
         * 返回诊断信息所在的行号。在此实现中始终返回 0。
         *
         * @return 行号信息，固定为 0
         */
        @Override
        public long getLineNumber() {
            return 0;
        }

        /**
         * 返回诊断信息所在的列号。在此实现中始终返回 0。
         *
         * @return 列号信息，固定为 0
         */
        @Override
        public long getColumnNumber() {
            return 0;
        }

        /**
         * 返回诊断信息的代码标识。在此实现中始终返回空字符串。
         *
         * @return 代码标识，固定为空字符串
         */
        @Override
        public String getCode() {
            return "";
        }

        /**
         * 返回诊断信息的消息内容。
         * 消息包括URL加载失败的错误提示以及相关的归档信息。
         *
         * @param locale 本地化语言环境
         * @return 格式化的错误消息字符串
         */
        @Override
        public String getMessage(Locale locale) {
            return System.lineSeparator() + "错误: 加载url失败   " + url + System.lineSeparator() + "   -->   " + archive + System.lineSeparator();
        }

        /**
         * 返回诊断信息的字符串表示形式。
         * 默认使用系统默认的语言环境生成消息。
         *
         * @return 诊断信息的字符串表示
         */
        @Override
        public String toString() {
            return this.getMessage(Locale.getDefault());
        }
    }

}
