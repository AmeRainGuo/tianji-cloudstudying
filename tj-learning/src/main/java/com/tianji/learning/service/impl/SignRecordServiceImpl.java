package com.tianji.learning.service.impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BooleanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {

    private final StringRedisTemplate redisTemplate;

    private final RabbitMqHelper rabbitMqHelper;

    @Override
    public SignResultVO addSignRecords() {
        // 1. 签到
        //获取登录用户
        Long userId = UserContext.getUser();
        //获取日期
        LocalDate now = LocalDate.now();
        //拼接key
        String key = String.format(String.format(RedisConstants.SIGN_RECORD_KEY_PREFIX
                + userId
                + now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER)));
        //计算offset
        int offset = now.getDayOfMonth() - 1;
        //保存签到信息
        Boolean exits = redisTemplate.opsForValue().setBit(key, offset, true);
        if (BooleanUtils.isTrue(exits)){
            throw new BizIllegalException("今天已经签到过了!");
        }
        //计算连续签到天数
        int signDays = countSignDays(key, now.getDayOfMonth());
        //计算签到得分
        int rewardPoints = 0;
        switch (signDays){
            case 7:
                rewardPoints = 10;
                break;
            case 14:
                rewardPoints = 20;
                break;
            case 28:
                rewardPoints = 40;
                break;
        }
        //保存积分明细记录
        rabbitMqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
                SignInMessage.of(userId, rewardPoints+1));

        //封装返回
        SignResultVO vo = new SignResultVO();
        vo.setSignDays(signDays);
        vo.setRewardPoints(rewardPoints);
        return vo;
    }

    private int countSignDays(String key, int len) {
        //获取本月从第一天开始 到今天位为止的签到记录
        List<Long> result = redisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(len))
                        .valueAt(0));
        if (CollUtils.isEmpty( result)){
            return 0;
        }
        int num = result.get(0).intValue();
        //定义一个计数器
        int count = 0;
        //循环 与1做与运算，得到最后一个bit，判断是否为0，为0则终止，为1则继续
        while ((num & 1) == 1){
            //计数器+1
            count++;
            //把数字右移一位，最后一位被舍弃，倒数第二位成为最后一位
            num >>>= 1;
        }
        return count;
    }

    @Override
    public List<Integer> querySignRecords() {
        //获取登录用户
        Long userId = UserContext.getUser();
        LocalDate now = LocalDate.now();
        //拼接key
        String key = String.format(String.format(RedisConstants.SIGN_RECORD_KEY_PREFIX
                + userId
                + now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER)));

        int len = now.getDayOfMonth();
        //获取本月从第一天开始 到今天位为止的签到记录
        List<Long> result = redisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(len))
                        .valueAt(0));

        long signBits = 0;
        if (CollUtils.isNotEmpty(result)) {
            signBits = result.get(0);
        }

        // 2. 计算当月的总天数（比如4月有30天，2月有28/29天）
        int daysInMonth = now.lengthOfMonth();

        List<Integer> signRecords = new ArrayList<>(daysInMonth);
        for (int i = 0; i < daysInMonth; i++) {
            // i 代表 offset，对应当月第 i+1 天
            if (i < len) {
                // 1号到今天：直接从Redis的bit里取状态
                // 注意：i是offset，最低位是今天，所以要用 (signBits >> (len - 1 - i)) & 1
                int sign = (int) ((signBits >> (len - 1 - i)) & 1);
                signRecords.add(sign);
            } else {
                // 今天之后的日期：默认补0（未签到）
                signRecords.add(0);
            }
        }
        return signRecords;
    }
}