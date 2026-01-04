/**
 * @author Lzpeng723
 * @description $END$
 * @since 2025/12/11 16:39
 */
public record B(ClassLoader classLoader) {

    public B(){
        this(B.class.getClassLoader());
    }

    public B {
        new A(classLoader);
    }
}
