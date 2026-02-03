package io.github.lzpeng.compiler.file;

import io.github.lzpeng.compiler.CharPool;
import io.github.lzpeng.compiler.util.IoUtil;
import io.github.lzpeng.compiler.util.UriUtil;

import javax.tools.SimpleJavaFileObject;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * StreamJavaFileObject类扩展了SimpleJavaFileObject，用于表示可以读写数据的Java文件对象。
 * 该类提供了多种构造方法来创建不同类型的文件对象，并支持通过输入输出流进行数据读写。
 * 它主要用于处理Java源代码或编译后的类文件等场景。
 */
/*sealed*/ class StreamJavaFileObject extends SimpleJavaFileObject /*permits JavaSourceFileObject, JavaClassFileObject*/ {

    /**
     * 代表此文件对象的源代码内容。
     * 此字段存储了与当前文件对象关联的源代码，可以是Java源代码或其他类型的文本数据。
     * 在实现中，这个CharSequence可能被用来读取或写入到相应的输入/输出流中。
     */
    protected CharSequence sourceCode;
    /**
     * 与当前文件对象关联的输入流，用于读取数据。
     * 此输入流通常在构造函数中被初始化，并且可以通过调用 {@link #openInputStream()} 方法来访问。
     *
     * @see #openInputStream()
     */
    protected InputStream inputStream;
    /**
     * 与此文件对象关联的输出流，用于写入数据到当前文件对象。
     * 该输出流在构造函数中被初始化，并可以通过调用 {@link #openOutputStream()} 方法来获取。
     */
    protected OutputStream outputStream;

    /**
     * 构造一个指定名称并关联给定输入流的StreamJavaFileObject。
     * <p>
     * 此构造函数首先调用另一个接受文件名和类型作为参数的构造函数来初始化基础信息，
     * 然后设置与该文件对象关联的输入流。此输入流可用于读取数据从当前文件对象。
     *
     * @param name        文件名，不包括扩展名。
     * @param inputStream 与此文件对象关联的输入流，用于读取数据。
     */
    protected StreamJavaFileObject(String name, InputStream inputStream) {
        this(name, Kind.SOURCE);
        this.inputStream = inputStream;
    }

    /**
     * 构造一个指定名称并关联给定输出流的StreamJavaFileObject。
     * <p>
     * 此构造函数首先调用另一个接受文件名和类型作为参数的构造函数来初始化基础信息，
     * 然后设置与该文件对象关联的输出流。此输出流可用于写入数据到当前文件对象。
     *
     * @param name         文件名，不包括扩展名。
     * @param outputStream 与此文件对象关联的输出流，用于写入数据。
     */
    protected StreamJavaFileObject(String name, OutputStream outputStream) {
        this(name, Kind.CLASS);
        this.outputStream = outputStream;
    }

    /**
     * 构造一个指定名称和类型的StreamJavaFileObject。
     * <p>
     * 该构造函数接受文件名和类型作为参数，并使用UriUtil.getStringURI方法将文件名转换为URI，
     * 然后调用父类构造函数创建一个新的SimpleJavaFileObject实例。文件名中的点（.）会被替换为斜杠（/），
     * 并附加给定类型的扩展名，以形成最终的URI字符串。
     *
     * @param name 文件名，不包括扩展名。
     * @param kind 文件类型，决定了文件的具体用途（如源代码、类文件等）。
     */
    protected StreamJavaFileObject(String name, Kind kind) {
        this(UriUtil.getStringURI(name.replace(CharPool.DOT, CharPool.SLASH) + kind.extension), kind);
    }

    /**
     * 构造一个指定URI和类型的StreamJavaFileObject。
     * <p>
     * 该构造函数接受URI和文件类型作为参数，用于创建一个新的StreamJavaFileObject实例。此构造函数主要用于内部使用或子类化时的扩展。
     *
     * @param uri  表示文件位置的URI
     * @param kind 文件类型，决定了文件的具体用途（如源代码、类文件等）
     */
    protected StreamJavaFileObject(URI uri, Kind kind) {
        super(uri, kind);
    }

    /**
     * 构造，支持File等路径类型的源码
     *
     * @param url 需要编译的文件uri
     */
    protected StreamJavaFileObject(URL url, Kind kind) throws URISyntaxException {
        this(url.toURI(), kind);
    }

    /**
     * 构造，支持File等路径类型的源码
     *
     * @param file 需要编译的文件uri
     */
    protected StreamJavaFileObject(File file, Kind kind) {
        this(file.toPath(), kind);
    }

    /**
     * 构造，支持File等路径类型的源码
     *
     * @param path 需要编译的文件uri
     */
    protected StreamJavaFileObject(Path path, Kind kind) {
        this(path.toUri(), kind);
        try {
            this.inputStream = Files.newInputStream(path);
            this.outputStream = Files.newOutputStream(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 构造
     *
     * @param className 编译后的class文件的类名
     * @see javax.tools.JavaFileManager#getJavaFileForOutput
     */
    protected StreamJavaFileObject(String className, byte[] bytes, Kind kind) {
        this(className, kind);
        this.inputStream = byteArrayToInputStream(bytes);
        this.outputStream = byteArrayToOutputStream(bytes);
    }

    /**
     * 将给定的字节数组转换为一个输入流。
     *
     * @param bytes 要转换成输入流的字节数组
     * @return 返回包含给定字节数组内容的ByteArrayInputStream实例
     */
    private static InputStream byteArrayToInputStream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    /**
     * 将给定的字节数组转换为一个输出流。
     *
     * @param bytes 要转换成输出流的字节数组
     * @return 返回包含给定字节数组内容的ByteArrayOutputStream实例
     */
    private static OutputStream byteArrayToOutputStream(byte[] bytes) {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // jdk8
        byteArrayOutputStream.write(bytes, 0, bytes.length);
        // jdk17
        //byteArrayOutputStream.writeBytes(bytes);
        return byteArrayOutputStream;
    }

    /**
     * 打开与此文件对象关联的输入流。
     *
     * @return 返回与当前文件对象关联的输入流
     * @throws IOException 如果在打开输入流时发生I/O错误
     */
    @Override
    public InputStream openInputStream() throws IOException {
        if (this.inputStream == null) {
            if (Kind.SOURCE.equals(this.kind)) {
                this.inputStream = toUri().toURL().openStream();
                return new BufferedInputStream(this.inputStream);
            }
            if (Kind.CLASS.equals(this.kind)) {
                try (OutputStream os = this.openOutputStream()) {
                    if (os instanceof ByteArrayOutputStream) {
                        final ByteArrayOutputStream byteArrayOutputStream = (ByteArrayOutputStream) os;
                        this.inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                    }
                    //if (os instanceof FileOutputStream) {
                    //
                    //}
                }
                return new BufferedInputStream(this.inputStream);
            }
            return super.openInputStream();
        }
        return this.inputStream;
    }

    /**
     * 打开输出流，用于写入数据到当前文件对象。
     *
     * @return 返回与当前文件对象关联的输出流
     * @throws IOException 如果在打开输出流时发生I/O错误
     */
    @Override
    public OutputStream openOutputStream() throws IOException {
        return this.outputStream;
    }

    /**
     * 返回此文件对象的字符内容。
     *
     * @param ignoreEncodingErrors 如果为true，则在读取过程中忽略编码错误
     * @return 包含文件内容的CharSequence
     * @throws IOException 如果在尝试打开输入流或读取文件内容时发生I/O错误
     */
    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        if (this.sourceCode == null) {
            try (final InputStream in = openInputStream()) {
                this.sourceCode = new String(IoUtil.readAllBytes(in));
            }
        }
        return this.sourceCode;
    }

}
