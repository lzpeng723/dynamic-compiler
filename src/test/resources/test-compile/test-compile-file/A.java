/**
 * @author Lzpeng723
 * @description $END$
 * @since 2025/12/11 16:38
 */
public class A { // Java8 用普通类替换 record

    // 对应原record的成员变量，私有不可变（final）
    private final ClassLoader classLoader;

    // 无参构造器：默认使用A类的类加载器
    public A() {
        this(A.class.getClassLoader());
    }

    // 全参构造器（对应原record的紧凑构造器，核心初始化逻辑）
    public A(ClassLoader classLoader) {
        this.classLoader = classLoader; // 初始化成员变量
        // 创建内部类实例，触发Runnable执行
        new InnerClass(new Runnable() {
            @Override
            public void run() {
                final StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
                System.out.println("初始化 " + A.this.getClass() + " 的调用链为: ");
                for (int i = 0; i < stackTraceElements.length; i++) {
                    final StackTraceElement stackTraceElement = stackTraceElements[i];
                    try {
                        System.out.printf("%-2d: %-60s %s%n",
                                i + 1,
                                classLoader.loadClass(stackTraceElement.getClassName()).getClassLoader(),
                                stackTraceElement);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    // 获取类加载器的getter方法（record默认自动生成，普通类需手动定义）
    public ClassLoader classLoader() {
        return classLoader;
    }

    // 内部类：Java8 用普通私有类替换record，保留构造器执行Runnable逻辑
    private class InnerClass {
        // 内部类成员变量
        private final Runnable runnable;

        // 内部类构造器：传入Runnable并立即执行
        public InnerClass(Runnable runnable) {
            this.runnable = runnable;
            this.runnable.run(); // 构造时执行run方法，与原逻辑一致
        }
    }
}