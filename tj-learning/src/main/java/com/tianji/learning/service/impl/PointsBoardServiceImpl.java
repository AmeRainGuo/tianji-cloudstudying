package com.tianji.learning.service.impl;

import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.utils.TableInfoContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author Amerain
 * @since 2026-04-26
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {

    private final RedisTemplate redisTemplate;

    private final UserClient userClient;

    public PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query) {
        // 1.判断是否是查询当前赛季
        Long season = query.getSeason();
        boolean isCurrent = season == null || season == 0;
        // 2.获取Redis的Key
        LocalDateTime now = LocalDateTime.now();
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        // 2.查询我的积分和排名
        PointsBoard myBoard = isCurrent ?
                queryMyCurrentBoard(key) : // 查询当前榜单（Redis）
                queryMyHistoryBoard(season); // 查询历史榜单（MySQL）
        // 3.查询榜单列表
        List<PointsBoard> list = isCurrent ?
                queryCurrentBoardList(key, query.getPageNo(), query.getPageSize()) :
                queryHistoryBoardList(query);
        // 4.封装VO
        PointsBoardVO vo = new PointsBoardVO();
        // 4.1.处理我的信息
        if (myBoard != null) {
            vo.setPoints(myBoard.getPoints());
            vo.setRank(myBoard.getRank());
        }
        if (CollUtils.isEmpty(list)) {
            return vo;
        }
        // 4.2.查询用户信息
        Set<Long> uIds = list.stream().map(PointsBoard::getUserId).collect(Collectors.toSet());
        List<UserDTO> users = userClient.queryUserByIds(uIds);
        Map<Long, String> userMap = new HashMap<>(users.size());
        if(CollUtils.isNotEmpty(users)) {
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));
        }
        // 4.3.处理榜单列表
        List<PointsBoardItemVO> items = new ArrayList<>(list.size());
        for (PointsBoard p : list) {
            PointsBoardItemVO v = new PointsBoardItemVO();
            items.add(v);
            v.setPoints(p.getPoints());
            v.setRank(p.getRank());
            v.setName(userMap.get(p.getUserId()));
        }
        vo.setBoardList(items);
        return vo;
    }


    private PointsBoard queryMyCurrentBoard(String key) {
        // 1.绑定key
        BoundZSetOperations<String, String> ops = redisTemplate.boundZSetOps(key);
        // 2.获取当前用户信息
        String userId = UserContext.getUser().toString();
        // 3.查询积分
        Double points = ops.score(userId);
        // 4.查询排名
        Long rank = ops.reverseRank(userId);
        // 5.封装返回
        PointsBoard p = new PointsBoard();
        p.setPoints(points == null ? 0 : points.intValue());
        p.setRank(rank == null ? 0 : rank.intValue() + 1);
        return p;
    }

    private PointsBoard queryMyHistoryBoard(Long season) {
        // 1. 关键：设置动态表名！
        TableInfoContext.setInfo("points_board_" + season);

        // 2. 获取当前用户信息
        String userId = UserContext.getUser().toString();

        // 3. 查询积分（此时MyBatis-Plus会自动替换为 points_board_xxx 表）
        PointsBoard board = lambdaQuery()
                .eq(PointsBoard::getUserId, userId)
                .one();

        // 4. 清理ThreadLocal（必须！否则线程复用会有脏数据）
        TableInfoContext.remove();

        // 5. 封装返回
        PointsBoard p = new PointsBoard();
        Integer points = board == null ? 0 : board.getPoints();
        Integer rank = board == null ? null : board.getRank();
        p.setPoints(points);
        p.setRank(rank == null ? 0 : rank + 1);
        return p;
    }

    @Override
    public List<PointsBoard> queryCurrentBoardList(String key, @Min(value = 1, message = "页码不能小于1") Integer pageNo, @Min(value = 1, message = "每页查询数量不能小于1") Integer pageSize) {
        int from = (pageNo - 1) * pageSize;
        // 2.查询
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, from, from + pageSize - 1);
        if (CollUtils.isEmpty(tuples)) {
            return CollUtils.emptyList();
        }
        // 3.封装
        int rank = from + 1;
        List<PointsBoard> list = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String userId = tuple.getValue();
            Double points = tuple.getScore();
            if (userId == null || points == null) {
                continue;
            }
            PointsBoard p = new PointsBoard();
            p.setUserId(Long.valueOf(userId));
            p.setPoints(points.intValue());
            p.setRank(rank++);
            list.add(p);
        }
        return list;
    }

    private List<PointsBoard> queryHistoryBoardList(PointsBoardQuery query) {

        Integer pageNo = query.getPageNo();
        Integer pageSize = query.getPageSize();
        Long season = query.getSeason();

        TableInfoContext.setInfo("points_board_" + season);


        // 2.查询
        int from = (pageNo - 1) * pageSize;
        // 2.查询
        List<PointsBoard> list = lambdaQuery()
                .eq(PointsBoard::getSeason, season)
                .orderByDesc(PointsBoard::getId)
                .list();

        if (CollUtils.isEmpty(list)) {
            return CollUtils.emptyList();
        }
        // 3.封装
        List<PointsBoard> backList = new ArrayList<>(list.size());
        for (PointsBoard pointsBoard : backList) {
            Long userId = pointsBoard.getUserId();
            Integer points = pointsBoard.getPoints();
            Long rank = pointsBoard.getId();
            if (userId == null || points == null) {
                continue;
            }
            PointsBoard p = new PointsBoard();
            p.setUserId(userId);
            p.setPoints(points);
            p.setRank(rank.intValue());
            backList.add(p);
        }
        return backList;

    }


    @Override
    public void createPointsBoardTableBySeason(Integer season) {
            getBaseMapper().createPointsBoardTable("points_board_" + season);
    }
}
