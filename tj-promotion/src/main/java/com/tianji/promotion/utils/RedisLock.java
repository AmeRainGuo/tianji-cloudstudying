package com.tianji.promotion.utils;

import com.tianji.common.utils.BooleanUtils;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@RequiredArgsConstructor
public class RedisLock {

    private String key;

    private final StringRedisTemplate redisTemplate;
    public boolean tryLock(long leaseTime, TimeUnit unit) {
        String value = Thread.currentThread().getName();

        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, leaseTime, unit);

        return BooleanUtils.isTrue(success);
    }

    public void unlock() {
        redisTemplate.delete(key);
    }
}
