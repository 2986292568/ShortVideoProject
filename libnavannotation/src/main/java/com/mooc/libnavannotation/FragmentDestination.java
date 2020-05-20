package com.mooc.libnavannotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/***
 * 创建了一个给Fragment使用的注解
 * 只能标记在类的头部上
 */
@Target(ElementType.TYPE)
public @interface FragmentDestination {
    //页面的身份标志
    String pageUrl();

    //是否需要登陆
    boolean needLogin() default false;

    // 是否把当前页面作为开始页面
    boolean asStarter() default false;
}
