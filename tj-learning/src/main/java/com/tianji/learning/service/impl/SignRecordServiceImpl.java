package com.tianji.learning.service.impl;

import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public SignResultVO addSignRecords() {
        // 1. 签到

        //计算连续签到天数

        //TODO 计算签到得分

        //TODO 保存积分明细记录

        //封装返回
        return null;
    }
}