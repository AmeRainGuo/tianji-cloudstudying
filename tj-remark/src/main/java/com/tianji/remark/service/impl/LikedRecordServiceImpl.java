package com.tianji.remark.service.impl;

import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author Amerain
 * @since 2026-04-24
 */
@RequiredArgsConstructor
//@Service
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    //    private final AmqpTemplate amqpTemplate;
    //利用common包下封装好的mq工具类，发送MQ通知 底层还是调用的rabbitmq的api
    private static RabbitMqHelper rabbitMqHelper;

    @Override
    public void addLikeRecord(LikeRecordFormDTO recordFormDTO) {
        //基于前端的参数，判断是执行点赞还是取消点赞
        boolean success = recordFormDTO.getLiked() ? like(recordFormDTO) : unlike(recordFormDTO);
        //判断是否执行成功，如果失败，则直接结束
        if (!success) {
            //失败，则直接结束
            return;
        }

        //如果执行成功，统计点赞总数
        Long likeTimes = lambdaQuery()
                .eq(LikedRecord::getBizId, recordFormDTO.getBizId())
                .count();
        //发送MQ通知
        rabbitMqHelper.send(LIKE_RECORD_EXCHANGE,
                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE, recordFormDTO.getBizType()),
                LikedTimesDTO.of(recordFormDTO.getBizId(), likeTimes.intValue()));
    }

    private boolean unlike(LikeRecordFormDTO recordFormDTO) {
        boolean result = remove(lambdaQuery()
                .eq(LikedRecord::getUserId, UserContext.getUser())
                .eq(LikedRecord::getBizId, recordFormDTO.getBizId()));
        return result;
    }

    private boolean like(LikeRecordFormDTO recordFormDTO) {
        //查询点赞记录
        Long count = lambdaQuery()
                .eq(LikedRecord::getUserId, UserContext.getUser())
                .eq(LikedRecord::getBizId, recordFormDTO.getBizId())
                .count();
        //判断是否存在，如果已经存在 则直接结束
        if (count > 0) {
            //如果存在，则直接结束
            return false;
        }
        LikedRecord likedRecord = BeanUtils.copyBean(recordFormDTO, LikedRecord.class);
        save(likedRecord);
        return true;
    }

    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        //获取当前用户ID
        Long userId = UserContext.getUser();
        //查询点赞记录
        List<LikedRecord> list = lambdaQuery()
                .in(LikedRecord::getBizId, bizIds)
                .eq(LikedRecord::getUserId, userId)
                .list();
        return list.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
    }

    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
        return;
    }
}