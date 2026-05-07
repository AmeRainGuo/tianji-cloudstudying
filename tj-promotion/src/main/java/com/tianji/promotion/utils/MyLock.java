package com.tianji.promotion.utils;

import com.tianji.common.autoconfigure.redisson.enums.LockType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

//标记注解作用的时间
@Retention(RetentionPolicy.RUNTIME)
//标记注解作用的元素类型为方法
@Target(ElementType.METHOD)
public @interface MyLock {
    String name();

    long waitTime() default 1;

    //默认租期为-1，表示不自动释放锁
    long leaseTime() default -1;

    TimeUnit unit() default TimeUnit.SECONDS;

    MyLockType lockType() default MyLockType.RE_ENTRANT_LOCK;

    MyLockStrategy lockStrategy() default MyLockStrategy.FAIL_AFTER_RETRY_TIMEOUT;
}
