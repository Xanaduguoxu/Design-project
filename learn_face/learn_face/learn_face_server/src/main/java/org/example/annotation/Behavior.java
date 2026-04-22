package org.example.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//  搜索行为
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Behavior {

    String value() default "";
}
