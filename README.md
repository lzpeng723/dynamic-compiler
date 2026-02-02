# dynamic-compiler

# Java 动态编译工具

轻量、易用的 Java 动态编译工具，基于 JDK 原生 JavaCompiler 封装，让 Java 拥有便捷的动态脚本执行能力

# 介绍

JDK 提供了 JavaCompiler 用于动态编译 java 源码文件，然后通过类加载器加载，这种动态编译可以让 Java有动态脚本的特性，突破静态编译语言的限制，在动态扩展业务逻辑、脚本化配置、运行时生成执行代码等场景下具备极高的实用价值。
 
但原生 JavaCompiler 存在使用门槛较高、依赖管理繁琐、注解处理器集成复杂、类加载器隔离不友好等问题，开发者需要手动处理编译参数拼接、输出目录管理、编译结果校验、依赖包引入等一系列繁琐操作。

本项目针对上述痛点，对 JDK 原生动态编译能力进行了一站式封装，提供简洁直观的 API接口，自动处理编译流程中的底层细节，同时支持依赖包引入、注解处理器（APT）集成、自定义类加载器等高级特性，让开发者能够以最少的代码快速实现Java 源码的动态编译与加载执行。

# 本项目特性：

1. 极简 API：一行代码即可完成基础 Java 源码编译与类加载
2. 零额外依赖：仅依赖 JDK 原生 API，无需引入第三方 Jar 包
3. 完善的场景支持：覆盖纯源码编译、带外部依赖编译、注解处理编译等核心场景
4. 灵活扩展：支持自定义编译输出目录、类加载器、编译参数等
5. 友好的异常提示：封装编译异常信息，快速定位源码语法或依赖问题

# 使用

前置要求
JDK 17 及以上版本
项目构建工具：Maven/Gradle（可选，用于快速引入本工具）

# 快速引入

## Maven 依赖

```xml

<dependency>
  <groupId>io.github.lzpeng723</groupId>
    <artifactId>dynamic-compiler</artifactId>
    <version>1.0.0-M2</version>
</dependency>
```

## Gradle 依赖

```groovy
implementation 'io.github.lzpeng723:dynamic-compiler:1.0.0-M2'
```

# 一、编译 Java 源码（基础无依赖场景）

该场景适用于无外部 Jar 依赖、无需注解处理的简单 Java 源码编译，支持单个源码字符串、多个源码文件批量编译、指定目录源码编译。

## 1. 核心 API 说明

核心工具类：[JavaSourceCompiler](./src/main/java/io/github/lzpeng/compiler/JavaSourceCompiler.java)
核心方法：

- static JavaSourceCompiler create()：创建编译器实例
- JavaSourceCompiler addSource(String className, String sourceCode)：添加单个源码字符串
- JavaSourceCompiler addSource(File sourceFile)：添加单个源码文件
- JavaSourceCompiler addSourceDirectory(File sourceDir)：添加指定目录下的所有源码文件
- ClassLoader compile()：执行编译，返回加载编译后类的类加载器
- ClassLoader compile(List<String> options)：使用指定的参数执行编译，返回加载编译后类的类加载器

## 2. 代码示例

### 2.1 编译单个源码字符串

```java
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
```

### 2.2 批量编译多个源码字符串

```java
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
```

### 2.3 编译指定源码文件

```java
/**
 * 测试编译指定目录下的 Java 源代码，并验证编译结果。
 * 该方法执行以下步骤：
 * - 使用 {@link JavaSourceCompiler} 创建一个编译器实例。
 * - 添加源码文件到编译器中。
 * - 编译源代码并获取生成的类加载器。
 * - 通过类加载器加载编译后的类，实例化并验证结果。
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
```

### 2.4 编译指定目录下的所有源码文件

```java
/**
 * 测试编译指定目录下的 Java 源代码，并验证编译结果。
 * 该方法执行以下步骤：
 * - 使用 {@link JavaSourceCompiler} 创建一个编译器实例。
 * - 添加源码目录和 Lombok 处理器到编译配置中。
 * - 编译源代码并获取生成的类加载器。
 * - 通过类加载器加载编译后的类，实例化并验证结果。
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
```

### 2.5 编译依赖当前环境类的源码

```java
/**
 * 测试编译包含当前环境类依赖的 Java 源代码并验证其执行结果。
 * 该方法执行以下步骤：
 * - 定义依赖当前项目中 JavaSourceCompiler 类的源码。
 * - 使用 {@link JavaSourceCompiler} 创建编译器实例并添加源码编译。
 * - 加载编译后的类，反射调用方法验证执行结果。
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
```

# 二、有依赖包的编译场景

该场景适用于待编译的 Java 源码依赖外部 Jar 包（如第三方框架、自定义工具包）的情况，本工具支持通过文件路径、URL 等方式引入依赖包。

## 1.核心 API 说明

- 依赖添加方法：
    - JavaSourceCompiler addDependency(String... dependencies)：通过 URL 地址添加依赖包
    - JavaSourceCompiler addDependency(File... files)：通过本地文件添加依赖包
    - JavaSourceCompiler addDependency(URL... urls)：通过 URL 数组添加依赖包
- 自定义类加载器初始化：static JavaSourceCompiler create(ClassLoader parentClassLoader)
- 依赖包会自动加入编译类路径和类加载器的资源路径，无需手动处理类路径拼接

## 2. 代码示例

### 2.1 通过本地文件添加依赖包

```java
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
            .addDependency(new File("../cn/hutool/hutool-all/5.8.42/hutool-all-5.8.42.jar"))
            .compile();
    final Class<?> clazz = classLoader.loadClass(className);
    final Method helloMethod = clazz.getDeclaredMethod("hello");
    helloMethod.invoke(null);
}
```

### 2.2 通过 URL 添加远程依赖包

```java
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
            .addDependency("https://repo1.maven.org/maven2/cn/hutool/hutool-all/5.8.42/hutool-all-5.8.42.jar")
            .compile();
    final Class<?> clazz = classLoader.loadClass(className);
    final Method helloMethod = clazz.getDeclaredMethod("hello");
    helloMethod.invoke(null);
}
```

### 2.3 自定义类加载器加载依赖

```java
/**
 * 测试编译包含外部依赖的 Java 源代码并验证其执行结果。
 * 该方法执行以下步骤：
 * - 定义依赖 hutool 库的 Java 源码，用于打印系统信息。
 * - 自定义 URLClassLoader 加载 hutool 依赖包。
 * - 使用自定义类加载器创建 JavaSourceCompiler 实例并编译源码。
 * - 反射调用方法验证依赖加载和执行结果。
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
    final URL url = new URL("https://repo1.maven.org/maven2/cn/hutool/hutool-all/5.8.42/hutool-all-5.8.42.jar");
    final URLClassLoader urlClassLoader = URLClassLoader.newInstance(new URL[]{url});
    final ClassLoader classLoader = JavaSourceCompiler.create(urlClassLoader)
            .addSource(className, sourceCode)
            .compile();
    final Class<?> clazz = classLoader.loadClass(className);
    final Method helloMethod = clazz.getDeclaredMethod("hello");
    helloMethod.invoke(null);
}
```

## 3. 关键说明

- 支持本地 Jar 文件、远程 Jar URL（如 Maven 仓库地址）两种依赖引入方式
- 支持自定义父类加载器，实现依赖的隔离加载

# 三、有注解处理程序（APT）的编译场景

该场景适用于待编译的 Java 源码包含自定义注解，且需要通过注解处理器（APT）在编译期生成辅助代码（如 MyBatis Mapper、Lombok
注解、自定义注解处理器）的情况。

## 1. 核心 API 说明

- 注解处理器添加：addProcessorPath 相关方法
  - JavaSourceCompiler addProcessorPath(boolean, java.io.File...)：通过文件添加注解处理器
  - JavaSourceCompiler addProcessorPath(boolean, URL...)：通过URL添加注解处理器
  - JavaSourceCompiler addProcessorPath(boolean, String...)：通过url字符串添加注解处理器
- 指定注解处理器参数：compiler.addProcessorPathOption("key", "value")（如指定 APT 生成代码的输出目录）
- 核心特性：自动识别注解处理器的 javax.annotation.processing.Processor 实现，无需手动配置 processor 编译参数

## 2. 代码示例

### 2.1 使用 Lombok 注解处理器

```java
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
```

### 2.2 指定编译的class文件输出路径

```java
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
```

# 四、异常场景测试示例

```java
/**
 * 测试编译包含错误的 Java 源代码并验证其行为。
 * 该方法用于验证编译器在源码依赖缺失时的异常处理能力。
 *
 * @throws Exception 如果在编译、类加载或方法调用过程中发生预期之外的异常
 */
@Test
void testCompileError() throws Exception {
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
    // 注意：此处未添加 hutool 依赖，编译会抛出异常
    final ClassLoader classLoader = JavaSourceCompiler.create()
            .addSource(className, sourceCode)
            .compile();
    final Class<?> clazz = classLoader.loadClass(className);
    final Method helloMethod = clazz.getDeclaredMethod("hello");
    helloMethod.invoke(null);
}
```

# 常见问题（FAQ）

编译失败提示类缺失：检查依赖包是否正确添加，确保依赖的 Jar 包路径 / URL 有效，且类名、包名与源码中导入的一致。

注解处理器不生效：确认注解处理器 Jar 包已正确添加，且处理器类实现了 javax.annotation.processing.Processor 接口。

类加载器冲突：使用自定义父类加载器隔离不同版本的依赖包，避免类加载冲突。

# 高级特性（自定义类加载器、编译缓存、模块化编译支持）

- 自定义编译输出目录：compiler.setClassOutput(new File("./classes")) 指定编译后的 Class 文件输出目录。

# 贡献指南

1. Fork 本项目
2. 创建特性分支 (git checkout -b feature/AmazingFeature)
3. 提交代码 (git commit -m 'Add some AmazingFeature')
4. 推送到分支 (git push origin feature/AmazingFeature)
5. 打开 Pull Request

# 许可证（License）

本项目采用 MIT 许可证开源，详情请查看 LICENSE 文件。
