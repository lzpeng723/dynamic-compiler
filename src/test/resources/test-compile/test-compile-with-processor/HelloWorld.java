package test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class HelloWorld {

    public static void hello() {
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