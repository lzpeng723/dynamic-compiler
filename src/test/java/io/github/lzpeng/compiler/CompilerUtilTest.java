package io.github.lzpeng.compiler;

import io.github.lzpeng.compiler.resource.ClassPathResource;
import io.github.lzpeng.compiler.util.CompilerUtil;
import org.junit.jupiter.api.*;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;

/**
 * CompilerUtilTest 是一个测试类，用于验证 {@link CompilerUtil} 类的功能。
 * 该测试类通过多个测试方法来确保编译 Java 源代码、加载编译后的类以及执行静态方法等操作的正确性。
 * 测试涵盖了单个类、多个类、包含外部依赖的类、使用 Lombok 注解处理器的情况以及其他相关场景。
 * @author lzpeng723
 */
@Tag("动态编译测试")
class CompilerUtilTest {


    /**
     * 测试编译 Java 源代码并验证其执行结果。
     * 该方法执行以下步骤：
     * - 定义一个简单的 Java 类源代码，包含一个静态方法用于打印 "Hello World"。
     * - 使用 {@link CompilerUtil} 创建一个编译器实例，并添加上述定义的源代码进行编译。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，确保方法能够正确执行且输出预期结果。
     *
     * @throws Exception 如果在编译、类加载或方法调用过程中发生异常
     */
    @Test
    @DisplayName("测试编译单个类")
    void testCompileCode() throws Exception {
        final String className = "test.HelloWorld";
        final String sourceCode = """
                package test;
                
                public class HelloWorld {
                    public static void hello(){
                        System.out.println("Hello World");
                    }
                }
                """;
        final ClassLoader classLoader = CompilerUtil.compile(className, sourceCode);
        final Class<?> clazz = classLoader.loadClass(className);
        final Method helloMethod = clazz.getDeclaredMethod("hello");
        helloMethod.invoke(null);
    }

    /**
     * 测试编译多个 Java 源代码文件并验证其执行结果。
     * 该方法执行以下步骤：
     * - 定义两个简单的 Java 类源代码，其中一个类调用另一个类中的静态方法。
     * - 使用 {@link CompilerUtil} 创建一个编译器实例，并添加上述定义的源代码进行编译。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，确保跨类的方法调用能够正确执行且输出预期结果。
     *
     * @throws Exception 如果在编译、类加载或方法调用过程中发生异常
     */
    @Test
    @DisplayName("测试编译多个类")
    void testCompileMultiCode() throws Exception {
        final String className1 = "test.HelloWorld";
        final String sourceCode1 = """
                package test;
                
                public class HelloWorld {
                    public static void hello(){
                        Demo.demo("Hello World");
                    }
                }
                """;
        final String className2 = "test.Demo";
        final String sourceCode2 = """
                package test;
                
                public class Demo {
                    public static void demo(String str){
                        System.out.println("Demo " + str);
                    }
                }
                """;
        final ClassLoader classLoader = CompilerUtil.create()
                .addSource(className1, sourceCode1)
                .addSource(className2, sourceCode2)
                .compile();
        final Class<?> clazz = classLoader.loadClass(className1);
        final Method helloMethod = clazz.getDeclaredMethod("hello");
        helloMethod.invoke(null);
    }


    /**
     * 测试编译包含外部依赖的 Java 源代码并验证其执行结果。
     * 该方法执行以下步骤：
     * - 定义一个简单的 Java 类源代码，其中导入了外部库 `cn.hutool.system.SystemUtil` 并使用它来打印系统信息。
     * - 使用 {@link CompilerUtil} 创建一个编译器实例，并添加上述定义的源代码进行编译。同时指定需要从 Maven 中央仓库下载的 hutool 库作为编译时依赖。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，确保方法能够正确执行且输出预期结果。
     *
     * @throws Exception 如果在编译、类加载或方法调用过程中发生异常
     */
    @Test
    @DisplayName("测试编译包含依赖")
    void testCompileWithDependency() throws Exception {
        final String className = "test.HelloWorld";
        final String sourceCode = """
                package test;
                
                import cn.hutool.system.SystemUtil;
                
                public class HelloWorld {
                    public static void hello(){
                        SystemUtil.dumpSystemInfo();
                    }
                }
                """;
        final ClassLoader classLoader = CompilerUtil.create()
                .addSource(className, sourceCode)
                .addDependencyPath("https://repo1.maven.org/maven2/cn/hutool/hutool-all/5.8.43/hutool-all-5.8.43.jar")
                .compile();
        final Class<?> clazz = classLoader.loadClass(className);
        final Method helloMethod = clazz.getDeclaredMethod("hello");
        helloMethod.invoke(null);
    }


    /**
     * 测试使用 Lombok 注解处理器编译 Java 源代码并验证其执行结果。
     * 该方法执行以下步骤：
     * - 定义一个包含 Lombok 注解（如 @Data 和 @Builder）的 Java 类源代码。
     * - 使用 {@link CompilerUtil} 创建一个编译器实例，并添加上述定义的源代码进行编译。同时指定从 Maven 中央仓库下载 Lombok 库作为注解处理器。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，确保 Lombok 注解能够被正确处理且输出预期结果。
     *
     * @throws Exception 如果在编译、类加载或方法调用过程中发生异常
     */
    @Test
    @DisplayName("测试编译包含Lombok")
    void testCompileWithLombok() throws Exception {
        final String className = "test.HelloWorld";
        final String sourceCode = """
                package test;
                
                import lombok.Builder;
                import lombok.Data;
                
                public class HelloWorld {
                    public static void hello(){
                		JavaBean javaBean = JavaBean.builder()
                			.name("lzpeng723")
                			.age(18)
                			.address("地球")
                			.build();
                        System.out.println(javaBean);
                    }
                }
                
                @Data
                @Builder
                class JavaBean {
                	private String name;
                	private int age;
                	private String address;
                }
                """;
        final ClassLoader classLoader = CompilerUtil.create()
                .addSource(className, sourceCode)
                .addProcessorPath(true, "https://repo1.maven.org/maven2/org/projectlombok/lombok/1.18.42/lombok-1.18.42.jar")
                .compile();
        final Class<?> clazz = classLoader.loadClass(className);
        final Method helloMethod = clazz.getDeclaredMethod("hello");
        helloMethod.invoke(null);
    }

    /**
     * 测试编译包含 Lombok 注解的 Java 源代码并验证其执行结果。
     * 该方法执行以下步骤：
     * - 定义一个包含 Lombok 注解（如 @Data 和 @Builder）的 Java 类源代码。
     * - 使用 {@link CompilerUtil} 创建一个编译器实例，并添加上述定义的源代码进行编译。同时指定从 Maven 中央仓库下载 Lombok 库作为注解处理器。
     * - 设置类输出目录为 "target/compile-classes" 并确保该目录存在。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，确保 Lombok 注解能够被正确处理且输出预期结果。
     *
     * @throws Exception 如果在编译、类加载或方法调用过程中发生异常
     */
    @Test
    @DisplayName("测试编译指定class文件输出")
    void testCompileCodeAndSetClassOutput() throws Exception {
        final String className = "test.HelloWorld";
        final String sourceCode = """
                package test;
                
                import lombok.Builder;
                import lombok.Data;
                
                public class HelloWorld {
                    public static void hello(){
                		JavaBean javaBean = JavaBean.builder()
                			.name("lzpeng723")
                			.age(18)
                			.address("地球")
                			.build();
                        System.out.println(javaBean);
                    }
                }
                
                @Data
                @Builder
                class JavaBean {
                	private String name;
                	private int age;
                	private String address;
                }
                """;
        final File compileClasses = new File("target/compile-classes");
        Files.createDirectories(compileClasses.toPath());
        final ClassLoader classLoader = CompilerUtil.create()
                .addSource(className, sourceCode)
                .addProcessorPath(true, "https://repo1.maven.org/maven2/org/projectlombok/lombok/1.18.42/lombok-1.18.42.jar")
                .setClassOutput(compileClasses)
                .compile();
        final Class<?> clazz = classLoader.loadClass(className);
        final Method helloMethod = clazz.getDeclaredMethod("hello");
        helloMethod.invoke(null);
    }


    /**
     * 测试编译指定目录下的 Java 源代码，并验证编译结果。
     * 该方法执行以下步骤：
     * - 使用 {@link CompilerUtil} 创建一个编译器实例。
     * - 添加源码路径和 Lombok 处理器到编译路径中。
     * - 编译源代码并获取生成的类加载器。
     * - 通过类加载器加载 TestLombok 类，调用其 create 方法创建对象。
     * - 验证创建的对象字符串表示以 "TestLombok(" 开头，确保编译与实例化正确无误。
     *
     * @throws Exception 如果在编译或类加载过程中出现任何异常
     */
    @Test
    @DisplayName("测试编译文件")
    void testCompileFile() throws Exception {
        final ClassLoader classLoader = CompilerUtil.compile(new ClassPathResource("test-compile/A.java").getFile());
        final Class<?> clazz = classLoader.loadClass("A");
        final Constructor<?> constructor = clazz.getConstructor(ClassLoader.class);
        final Object a = constructor.newInstance(classLoader);
        System.out.println("a = " + a);
        Assertions.assertTrue(String.valueOf(a).startsWith("A["));
    }


    /**
     * 测试编译指定目录下的 Java 源代码，并验证编译结果。
     * 该方法执行以下步骤：
     * - 使用 {@link CompilerUtil} 创建一个编译器实例。
     * - 添加源码路径和 Lombok 处理器到编译路径中。
     * - 编译源代码并获取生成的类加载器。
     * - 通过类加载器加载 TestLombok 类，调用其 create 方法创建对象。
     * - 验证创建的对象字符串表示以 "TestLombok(" 开头，确保编译与实例化正确无误。
     *
     * @throws Exception 如果在编译或类加载过程中出现任何异常
     */
    @Test
    @DisplayName("测试编译文件夹")
    void testCompileDirectory() throws Exception {
        final ClassLoader classLoader = CompilerUtil.compile(new ClassPathResource("test-compile").getFile());
        final Class<?> clazz = classLoader.loadClass("C");
        final Object c = clazz.getConstructor().newInstance();
        System.out.println("c = " + c);
        Assertions.assertTrue(String.valueOf(c).startsWith("C["));
    }


    /**
     * 测试编译包含外部依赖的 Java 源代码并验证其执行结果。
     * 该方法执行以下步骤：
     * - 定义一个简单的 Java 类源代码，其中导入了外部库 `cn.hutool.system.SystemUtil` 并使用它来打印系统信息。
     * - 使用 {@link CompilerUtil} 创建一个编译器实例，并添加上述定义的源代码进行编译。同时指定需要从 Maven 中央仓库下载的 hutool 库作为编译时依赖。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，确保方法能够正确执行且输出预期结果。
     *
     * @throws Exception 如果在编译、类加载或方法调用过程中发生异常
     */
    @Test
    @DisplayName("测试编译指定类加载器")
    void testCompileWithClassLoader() throws Exception {
        final String className = "test.HelloWorld";
        final String sourceCode = """
                package test;
                
                import cn.hutool.system.SystemUtil;
                
                public class HelloWorld {
                    public static void hello(){
                        SystemUtil.dumpSystemInfo();
                    }
                }
                """;
        final URL url = new URL("https://repo1.maven.org/maven2/cn/hutool/hutool-all/5.8.43/hutool-all-5.8.43.jar");
        final URLClassLoader urlClassLoader = URLClassLoader.newInstance(new URL[]{url});
        final ClassLoader classLoader = CompilerUtil.create(urlClassLoader)
                .addSource(className, sourceCode)
                .compile();
        final Class<?> clazz = classLoader.loadClass(className);
        final Method helloMethod = clazz.getDeclaredMethod("hello");
        helloMethod.invoke(null);
    }

    /**
     * 测试编译包含外部依赖的 Java 源代码并验证其执行结果。
     * 该方法执行以下步骤：
     * - 定义一个简单的 Java 类源代码，其中导入了外部库 `cn.hutool.system.SystemUtil` 并使用它来打印系统信息。
     * - 使用 {@link CompilerUtil} 创建一个编译器实例，并添加上述定义的源代码进行编译。同时指定需要从 Maven 中央仓库下载的 hutool 库作为编译时依赖。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，确保方法能够正确执行且输出预期结果。
     *
     * @throws Exception 如果在编译、类加载或方法调用过程中发生异常
     */
    @Test
    @DisplayName("测试编译使用当前环境信息")
    void testCompileWithCurrentEnv() throws Exception {
        final String className = "test.HelloWorld";
        final String sourceCode = """
                package test;
                
                import io.github.lzpeng.compiler.util.CompilerUtil;
                
                public class HelloWorld {
                    public static void hello(){
                        System.out.println("CompilerUtil = " + CompilerUtil.create());
                    }
                }
                """;
        final ClassLoader classLoader = CompilerUtil.create()
                .addSource(className, sourceCode)
                .compile();
        final Class<?> clazz = classLoader.loadClass(className);
        final Method helloMethod = clazz.getDeclaredMethod("hello");
        helloMethod.invoke(null);
    }

    /**
     * 测试编译包含外部依赖的 Java 源代码并验证其执行结果。
     * 该方法执行以下步骤：
     * - 定义一个简单的 Java 类源代码，其中导入了外部库 `cn.hutool.system.SystemUtil` 并使用它来打印系统信息。
     * - 使用 {@link CompilerUtil} 创建一个编译器实例，并添加上述定义的源代码进行编译。同时指定需要从 Maven 中央仓库下载的 hutool 库作为编译时依赖。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，确保方法能够正确执行且输出预期结果。
     *
     * @throws Exception 如果在编译、类加载或方法调用过程中发生异常
     */
    @Test
    @DisplayName("测试编译指定注解处理器")
    void testCompileWithProcessor() throws Exception {
        final String className = "test.HelloWorld";
        final String sourceCode = """
                package test;
                
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;
                
                public class HelloWorld {
                
                    public static void hello(){
                        System.out.println(new JavaBeanAudit());
                    }
                
                }
                
                @WithAudit
                class JavaBean {
                	private String name;
                	private int age;
                	private String address;
                }
                
                /**
                 * 标注该注解的JavaBean会自动添加审计字段和完整的toString()方法
                 * 日期类型使用JDK8原生的java.time.LocalDate（仅日期，无时间）
                 */
                @Target(ElementType.TYPE)
                @Retention(RetentionPolicy.SOURCE)
                @interface WithAudit {
                }
                """;
        final File compileSources = new File("target/compile-sources");
        Files.createDirectories(compileSources.toPath());
        final ClassLoader classLoader = CompilerUtil.create()
                .addSource(className, sourceCode)
                .addProcessor(new AuditProcessor())
                .setSourceOutput(compileSources)
                .compile();
        final Class<?> clazz = classLoader.loadClass(className);
        final Method helloMethod = clazz.getDeclaredMethod("hello");
        helloMethod.invoke(null);
    }

    /**
     * 测试编译包含错误的 Java 源代码并验证其行为。
     * 该方法执行以下步骤：
     * - 定义一个简单的 Java 类源代码，其中导入了外部库 `cn.hutool.system.SystemUtil` 并使用它来打印系统信息。
     * - 使用 {@link CompilerUtil} 创建一个编译器实例，并添加上述定义的源代码进行编译。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，期望捕获由编译或运行时错误导致的异常。
     *
     */
    @Test
    @DisplayName("测试编译错误")
    void testCompileError() {
        try {
            final String className = "test.HelloWorld";
            final String sourceCode = """
                    package test;
                    
                    import cn.hutool.system.SystemUtil;
                    
                    public class HelloWorld {
                        public static void hello(){
                            SystemUtil.dumpSystemInfo();
                        }
                    }
                    """;
            final ClassLoader classLoader = CompilerUtil.create()
                    .addSource(className, sourceCode)
                    .compile();
            final Class<?> clazz = classLoader.loadClass(className);
            final Method helloMethod = clazz.getDeclaredMethod("hello");
            helloMethod.invoke(null);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            Assertions.assertInstanceOf(CompilerException.class, e);
        }
    }


}