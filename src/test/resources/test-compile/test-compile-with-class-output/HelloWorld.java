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