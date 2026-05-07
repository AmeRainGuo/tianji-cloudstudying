package com.tianji.promotion.utils;


import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class MyLockFactory {

    private final RedissonClient redissonClient;
    private final Map<MyLockType, Function<String, RLock>> lockHandlers;

    public MyLockFactory(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        this.lockHandlers = new EnumMap<MyLockType,Function<String, RLock>>(MyLockType.class);
        this.lockHandlers.put(MyLockType.RE_ENTRANT_LOCK, redissonClient::getLock);
        this.lockHandlers.put(MyLockType.FAIR_LOCK, redissonClient::getFairLock);
        this.lockHandlers.put(MyLockType.READ_LOCK, name -> redissonClient.getReadWriteLock(name).readLock());
        this.lockHandlers.put(MyLockType.WRITE_LOCK, name -> redissonClient.getReadWriteLock(name).writeLock());
    }

    public RLock getLock(MyLockType lockType, String name) {
        return lockHandlers.get(lockType).apply(name);
    }
}
