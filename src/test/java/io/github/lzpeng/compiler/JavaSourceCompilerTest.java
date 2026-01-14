package io.github.lzpeng.compiler;

import io.github.lzpeng.compiler.resource.ClassPathResource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 * @author lzpeng723
 */

class JavaSourceCompilerTest {


    /**
     * 测试编译 Java 源代码并验证其执行结果。
     * 该方法执行以下步骤：
     * - 定义一个简单的 Java 类源代码，包含一个静态方法用于打印 "Hello World"。
     * - 使用 {@link JavaSourceCompiler} 创建一个编译器实例，并添加上述定义的源代码进行编译。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，确保方法能够正确执行且输出预期结果。
     *
     * @throws Exception 如果在编译、类加载或方法调用过程中发生异常
     */
    @Test
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
        final ClassLoader classLoader = JavaSourceCompiler.create()
                .addSource(className, sourceCode)
                .compile();
        final Class<?> clazz = classLoader.loadClass(className);
        final Method helloMethod = clazz.getDeclaredMethod("hello");
        helloMethod.invoke(null);
    }

    /**
     * 测试编译多个 Java 源代码文件并验证其执行结果。
     * 该方法执行以下步骤：
     * - 定义两个简单的 Java 类源代码，其中一个类调用另一个类中的静态方法。
     * - 使用 {@link JavaSourceCompiler} 创建一个编译器实例，并添加上述定义的源代码进行编译。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，确保跨类的方法调用能够正确执行且输出预期结果。
     *
     * @throws Exception 如果在编译、类加载或方法调用过程中发生异常
     */
    @Test
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
        final ClassLoader classLoader = JavaSourceCompiler.create()
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
     * - 使用 {@link JavaSourceCompiler} 创建一个编译器实例，并添加上述定义的源代码进行编译。同时指定需要从 Maven 中央仓库下载的 hutool 库作为编译时依赖。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，确保方法能够正确执行且输出预期结果。
     *
     * @throws Exception 如果在编译、类加载或方法调用过程中发生异常
     */
    @Test
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
        final ClassLoader classLoader = JavaSourceCompiler.create()
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
     * - 使用 {@link JavaSourceCompiler} 创建一个编译器实例，并添加上述定义的源代码进行编译。同时指定从 Maven 中央仓库下载 Lombok 库作为注解处理器。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，确保 Lombok 注解能够被正确处理且输出预期结果。
     *
     * @throws Exception 如果在编译、类加载或方法调用过程中发生异常
     */
    @Test
    void testCompileWithApt() throws Exception {
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
        final ClassLoader classLoader = JavaSourceCompiler.create()
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
     * - 使用 {@link JavaSourceCompiler} 创建一个编译器实例，并添加上述定义的源代码进行编译。同时指定从 Maven 中央仓库下载 Lombok 库作为注解处理器。
     * - 设置类输出目录为 "target/compile-classes" 并确保该目录存在。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，确保 Lombok 注解能够被正确处理且输出预期结果。
     *
     * @throws Exception 如果在编译、类加载或方法调用过程中发生异常
     */
    @Test
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
        compileClasses.mkdirs();
        final ClassLoader classLoader = JavaSourceCompiler.create()
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
     * - 使用 {@link JavaSourceCompiler} 创建一个编译器实例。
     * - 添加源码路径和 Lombok 处理器到编译路径中。
     * - 编译源代码并获取生成的类加载器。
     * - 通过类加载器加载 TestLombok 类，调用其 create 方法创建对象。
     * - 验证创建的对象字符串表示以 "TestLombok(" 开头，确保编译与实例化正确无误。
     *
     * @throws Exception 如果在编译或类加载过程中出现任何异常
     */
    @Test
    void testCompileFile() throws Exception {
        final ClassLoader classLoader = JavaSourceCompiler.create()
                .addSource(new ClassPathResource("test-compile/A.java").getFile())
                .compile();
        final Class<?> clazz = classLoader.loadClass("A");
        final Constructor<?> constructor = clazz.getConstructor(ClassLoader.class);
        final Object a = constructor.newInstance(classLoader);
        System.out.println("a = " + a);
        Assertions.assertTrue(String.valueOf(a).startsWith("A["));
    }


    /**
     * 测试编译指定目录下的 Java 源代码，并验证编译结果。
     * 该方法执行以下步骤：
     * - 使用 {@link JavaSourceCompiler} 创建一个编译器实例。
     * - 添加源码路径和 Lombok 处理器到编译路径中。
     * - 编译源代码并获取生成的类加载器。
     * - 通过类加载器加载 TestLombok 类，调用其 create 方法创建对象。
     * - 验证创建的对象字符串表示以 "TestLombok(" 开头，确保编译与实例化正确无误。
     *
     * @throws Exception 如果在编译或类加载过程中出现任何异常
     */
    @Test
    void testCompileDirectory() throws Exception {
        final ClassLoader classLoader = JavaSourceCompiler.create()
                .addSourceDirectory(new ClassPathResource("test-compile").getFile())
                .addProcessorPath(true, "https://repo1.maven.org/maven2/org/projectlombok/lombok/1.18.42/lombok-1.18.42.jar")
                .compile();
        final Class<?> clazz = classLoader.loadClass("C");
        final Object c = clazz.getConstructor().newInstance();
        System.out.println("c = " + c);
        Assertions.assertTrue(String.valueOf(c).startsWith("C["));
    }


    /**
     * 测试编译包含外部依赖的 Java 源代码并验证其执行结果。
     * 该方法执行以下步骤：
     * - 定义一个简单的 Java 类源代码，其中导入了外部库 `cn.hutool.system.SystemUtil` 并使用它来打印系统信息。
     * - 使用 {@link JavaSourceCompiler} 创建一个编译器实例，并添加上述定义的源代码进行编译。同时指定需要从 Maven 中央仓库下载的 hutool 库作为编译时依赖。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，确保方法能够正确执行且输出预期结果。
     *
     * @throws Exception 如果在编译、类加载或方法调用过程中发生异常
     */
    @Test
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
        final ClassLoader classLoader = JavaSourceCompiler.create(urlClassLoader)
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
     * - 使用 {@link JavaSourceCompiler} 创建一个编译器实例，并添加上述定义的源代码进行编译。同时指定需要从 Maven 中央仓库下载的 hutool 库作为编译时依赖。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，确保方法能够正确执行且输出预期结果。
     *
     * @throws Exception 如果在编译、类加载或方法调用过程中发生异常
     */
    @Test
    void testCompileWithCurrentEnv() throws Exception {
        final String className = "test.HelloWorld";
        final String sourceCode = """
                package test;
                
                import io.github.lzpeng.compiler.JavaSourceCompiler;
                
                public class HelloWorld {
                    public static void hello(){
                        System.out.println("JavaSourceCompiler = " + JavaSourceCompiler.create());
                    }
                }
                """;
        final ClassLoader classLoader = JavaSourceCompiler.create()
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
     * - 使用 {@link JavaSourceCompiler} 创建一个编译器实例，并添加上述定义的源代码进行编译。同时指定需要从 Maven 中央仓库下载的 hutool 库作为编译时依赖。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，确保方法能够正确执行且输出预期结果。
     *
     * @throws Exception 如果在编译、类加载或方法调用过程中发生异常
     */
    @Test
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
        compileSources.mkdirs();
        final ClassLoader classLoader = JavaSourceCompiler.create()
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
     * - 使用 {@link JavaSourceCompiler} 创建一个编译器实例，并添加上述定义的源代码进行编译。
     * - 编译完成后，通过生成的类加载器加载编译后的类。
     * - 反射调用加载类中的静态方法，期望捕获由编译或运行时错误导致的异常。
     *
     */
    @Test
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
            final ClassLoader classLoader = JavaSourceCompiler.create()
                    .addSource(className, sourceCode)
                    .compile();
            final Class<?> clazz = classLoader.loadClass(className);
            final Method helloMethod = clazz.getDeclaredMethod("hello");
            helloMethod.invoke(null);
        } catch (Exception e) {
            Assertions.assertInstanceOf(CompilerException.class, e);
        }
    }


}