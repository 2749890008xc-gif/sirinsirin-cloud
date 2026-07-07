package com.sirinsirin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})   //  @Target 用于描述注解的使用范围，即：被描述的注解可以用在什么地方。如：类、方法、属性
@Retention(RetentionPolicy.RUNTIME)
public @interface GlobalInterceptor {
    boolean checkLogin() default false;
}
