package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.service.IPointsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author Amerain
 * @since 2026-04-26
 */
@RequiredArgsConstructor
@Service
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {

    private final RedisTemplate redisTemplate;

    @Override
    public void addPointsRecord(Long userId, int points, PointsRecordType pointsRecordType) {
        int maxPoints = pointsRecordType.getMaxPoints();
        //判断当前方式有没有积分上限
        int realPoints = points;
        LocalDateTime now = LocalDateTime.now();
        if(maxPoints > 0){
            //有，则需要判断是否超过积分上限

            LocalDateTime begin = DateUtils.getDayStartTime(now);
            LocalDateTime end = DateUtils.getDayEndTime(now);
//            //查询今日已获得的积分
//            lambdaQuery().eq(PointsRecord::getUserId, userId)
//                    .eq(PointsRecord::getType, pointsRecordType)
//                    .eq(PointsRecord::getUserId, userId)
//                    .between(PointsRecord::getCreateTime, begin , end);
            int currentPoints = queryUserPointsByTypeAndDate(userId, pointsRecordType, begin, end);
            //判断是否超过上限
            if(currentPoints >= maxPoints){
                //超过 直接结束
                return;
            }

            //没超过 保存积分记录
            if (currentPoints + points > maxPoints){
               realPoints = maxPoints - currentPoints;
            }

        }

        //没有 直接保存积分记录
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType(pointsRecordType);
        record.setPoints(realPoints);
        save( record);
        //累计积分数据到redis的sortedSet中
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        redisTemplate.opsForZSet().incrementScore(key, userId.toString(), realPoints);

    }

    private int queryUserPointsByTypeAndDate(Long userId, PointsRecordType pointsRecordType, LocalDateTime begin, LocalDateTime end) {

        QueryWrapper<PointsRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(PointsRecord::getUserId, userId)
                .eq(pointsRecordType != null,PointsRecord::getType, pointsRecordType)
                .between(begin !=  null && end != null, PointsRecord::getCreateTime, begin , end);
        Integer points = getBaseMapper().queryUserPointsByTypeAndDate(queryWrapper) ;

        return points == null ? 0 : points;
    }

    @Override
    public List<PointsStatisticsVO> queryMyPointsToday() {
        // 1. 获取用户
        Long userId = UserContext.getUser();
        // 2. 获取日期
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime begin = DateUtils.getDayStartTime(now);
        LocalDateTime end = DateUtils.getDayEndTime(now);
        // 3. 构建查询条件
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(PointsRecord::getUserId, userId)
                .between(PointsRecord::getCreateTime, begin, end);
        // 4. 查询
        List<PointsRecord> list = getBaseMapper().queryUserPointsByDate(wrapper);
        if (CollUtils.isEmpty(list)){
            return CollUtils.emptyList();
        }
        // 5. 封装返回
        List<PointsStatisticsVO> vos = list.stream().map(record -> {
            PointsStatisticsVO vo = new PointsStatisticsVO();
            vo.setType(record.getType().getDesc());
            vo.setPoints(record.getPoints());
            vo.setMaxPoints(record.getType().getMaxPoints());
            return vo;
        }).collect(Collectors.toList());
        return vos;
    }
}
