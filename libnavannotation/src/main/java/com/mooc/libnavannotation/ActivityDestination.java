package com.mooc.libnavannotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/***
 * 创建一个给activity使用的注解
 */
@Target(ElementType.TYPE)
public @interface ActivityDestination {
    //页面身份标志
    String pageUrl();

    //是否需要登陆
    boolean needLogin() default false;

    //是否作为开始界面
    boolean asStarter() default false;
}
