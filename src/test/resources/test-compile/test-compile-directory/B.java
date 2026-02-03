import org.checkerframework.checker.units.qual.A;

/**
 * @author Lzpeng723
 * @description $END$
 * @since 2025/12/11 16:39
 */
public class B {
    // 对应原record的classLoader参数，私有不可变（final保证与record行为一致）
    private final ClassLoader classLoader;

    // 无参构造器：默认使用B类的类加载器，与原逻辑一致
    public B() {
        this(B.class.getClassLoader());
    }

    // 全参构造器（对应原record的紧凑构造器）：初始化类加载器并创建A实例
    public B(ClassLoader classLoader) {
        this.classLoader = classLoader; // 初始化成员变量
        new A(classLoader); // 核心逻辑：创建A实例，复用已有A类的调用链打印功能
    }

    // 手动添加getter方法（record会自动生成，普通类需显式定义，保证外部可访问属性）
    public ClassLoader classLoader() {
        return classLoader;
    }
}