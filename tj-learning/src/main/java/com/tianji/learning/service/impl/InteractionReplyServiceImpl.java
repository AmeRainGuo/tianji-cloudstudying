package com.tianji.learning.service.impl;

import cn.hutool.db.PageResult;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author Amerain
 * @since 2026-04-23
 */
@RequiredArgsConstructor
@Service
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {

    private final InteractionQuestionMapper questionMapper;

    private final UserClient userClient;

    private final RabbitMqHelper rabbitMqHelper;
    @Override
    public void addReply(ReplyDTO replyDTO) {
        Long userId = UserContext.getUser();

        InteractionReply newReply = BeanUtils.copyBean(replyDTO,InteractionReply.class);
        newReply.setUserId(userId);
        newReply.setCreateTime(LocalDateTime.now());
        newReply.setUpdateTime(LocalDateTime.now());

        this.save(newReply);
        // 判断是回答还是评论
        // 回答：answerId为空，targetReplyId为空
        // 评论：answerId为空，targetReplyId不为空
        if(replyDTO.getAnswerId()==null && replyDTO.getTargetReplyId()==null && replyDTO.getTargetUserId()==null){
            // 回答
            //修改问题表最近一次回答的id
            // 1. 构建更新条件和字段 累计问题下的回答次数
            LambdaUpdateWrapper<InteractionQuestion> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(InteractionQuestion::getId, replyDTO.getQuestionId())
                    .set(InteractionQuestion::getLatestAnswerId, newReply.getId())
                    .setSql("answer_times = answer_times + 1");

            // 2. 调用 Mapper 的 update 方法
            questionMapper.update(null, wrapper);

        }else{
            // 评论
            // 新增评论
            // 1. 构建更新条件和字段 累计问题下的评论次数
            this.lambdaUpdate()
                    .eq(InteractionReply::getId, replyDTO.getAnswerId())
                    .setSql("reply_times = reply_times + 1")
                    .update();
        }

        LambdaUpdateWrapper<InteractionQuestion> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(InteractionQuestion::getId, replyDTO.getQuestionId())
                .set(replyDTO.getIsStudent() == true ,InteractionQuestion::getStatus, 0);

        rabbitMqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.WRITE_REPLY,
                userId);

        questionMapper.update(null, wrapper);
    }

    @Override
    public PageDTO<ReplyVO> queryAdminReplyList(ReplyPageQuery pageQuery) {
        //参数校验 课程id和小节id不能同时为空
        Long questionId = pageQuery.getQuestionId();
        Long answerId = pageQuery.getAnswerId();

        if (questionId == null && answerId == null) {
            throw new IllegalArgumentException("问题id和回答id不能同时为空");
        }

        if(answerId == null){
            // 根据问题id查询回答
            Page<InteractionReply> pageResult = lambdaQuery()
                    .eq(InteractionReply::getQuestionId, questionId)
                    .eq(InteractionReply::getAnswerId, 0)
                    .page(pageQuery.toMpPage("liked_times", false));

            List<InteractionReply> records = pageResult.getRecords();
            if (CollUtils.isEmpty(records)) {
                return PageDTO.empty(pageResult);
            }
            Set<Long> userIds = records.stream().map(InteractionReply::getUserId).collect(Collectors.toSet());

            // 3.2. 根据id查询用户
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            Map<Long, UserDTO> userMap = new HashMap<>(users.size());
            if (CollUtils.isNotEmpty(users)) {
                userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
            }

            // 4. 封装VO
            List<ReplyVO> voList = new ArrayList<>(records.size());
            for (InteractionReply q : records) {
                // 4.1. 将PO转VO，属性拷贝
                ReplyVO vo = BeanUtils.copyBean(q, ReplyVO.class);
                voList.add(vo);

                // 4.2. 用户信息
                UserDTO user = userMap.get(q.getUserId());
                if (user != null) {
                    vo.setUserName(user.getName());
                    vo.setUserIcon(user.getIcon());
                }
            }
            return PageDTO.of(pageResult, voList);
        }else {
            // 根据回答id查询评论
            Page<InteractionReply> pageResult = lambdaQuery()
                    .eq(InteractionReply::getAnswerId, answerId)
                    .page(pageQuery.toMpPage("liked_times", false));

            List<InteractionReply> records = pageResult.getRecords();
            if (CollUtils.isEmpty(records)) {
                return PageDTO.empty(pageResult);
            }

            Set<Long> userIds = records.stream().map(InteractionReply::getUserId).collect(Collectors.toSet());
            Set<Long> targetUserIds = records.stream().map(InteractionReply::getTargetUserId).collect(Collectors.toSet());

            // 3.2. 根据id查询评论用户 和评论目标用户
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            List<UserDTO> targetUsers = userClient.queryUserByIds(targetUserIds);;
            Map<Long, UserDTO> userMap = new HashMap<>(users.size());
            Map<Long, UserDTO> targetUserMap = new HashMap<>(targetUsers.size());
            if (CollUtils.isNotEmpty(users)) {
                userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
                targetUserMap = targetUsers.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
            }

            // 4. 封装VO
            List<ReplyVO> voList = new ArrayList<>(records.size());
            for (InteractionReply q : records) {
                // 4.1. 将PO转VO，属性拷贝
                ReplyVO vo = BeanUtils.copyBean(q, ReplyVO.class);
                voList.add(vo);

                // 4.2. 用户信息
                UserDTO user = userMap.get(q.getUserId());
                UserDTO targetUser = targetUserMap.get(q.getTargetUserId());
                if (user != null) {
                    vo.setUserName(user.getName());
                    vo.setUserIcon(user.getIcon());
                    vo.setTargetUserName(targetUser.getName());
                }

            }
            return PageDTO.of(pageResult, voList);
        }
    }

    @Override
    public PageDTO<ReplyVO> queryReplyList(ReplyPageQuery pageQuery) {
        //参数校验 课程id和小节id不能同时为空
        Long questionId = pageQuery.getQuestionId();
        Long answerId = pageQuery.getAnswerId();

        if (questionId == null && answerId == null) {
            throw new IllegalArgumentException("问题id和回答id不能同时为空");
        }

        if(answerId == null){
            // 根据问题id查询回答
            Page<InteractionReply> pageResult = lambdaQuery()
                    .eq(InteractionReply::getQuestionId, questionId)
                    .eq(InteractionReply::getAnswerId, 0)
                    .eq(InteractionReply::getHidden, false)
                    .page(pageQuery.toMpPage("liked_times", false));

            List<InteractionReply> records = pageResult.getRecords();
            if (CollUtils.isEmpty(records)) {
                return PageDTO.empty(pageResult);
            }
            Set<Long> userIds = records.stream().map(InteractionReply::getUserId).collect(Collectors.toSet());

            // 3.2. 根据id查询用户
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            Map<Long, UserDTO> userMap = new HashMap<>(users.size());
            if (CollUtils.isNotEmpty(users)) {
                userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
            }

            // 4. 封装VO
            List<ReplyVO> voList = new ArrayList<>(records.size());
            for (InteractionReply q : records) {
                // 4.1. 将PO转VO，属性拷贝
                ReplyVO vo = BeanUtils.copyBean(q, ReplyVO.class);
                voList.add(vo);

                // 4.2. 用户信息
                UserDTO user = userMap.get(q.getUserId());
                if (user != null) {
                    if(vo.getAnonymity() == false) {
                        vo.setUserName(user.getName());
                        vo.setUserIcon(user.getIcon());
                    }else{
                        vo.setUserName(null);
                        vo.setUserIcon(null);
                    }
                }
            }
            return PageDTO.of(pageResult, voList);
        }else {
            // 根据回答id查询评论
            Page<InteractionReply> pageResult = lambdaQuery()
                    .eq(InteractionReply::getAnswerId, answerId)
                    .eq(InteractionReply::getHidden, false)
                    .page(pageQuery.toMpPage("liked_times", false));

            List<InteractionReply> records = pageResult.getRecords();
            if (CollUtils.isEmpty(records)) {
                return PageDTO.empty(pageResult);
            }

            Set<Long> userIds = records.stream().map(InteractionReply::getUserId).collect(Collectors.toSet());
            Set<Long> targetUserIds = records.stream().map(InteractionReply::getTargetUserId).collect(Collectors.toSet());

            // 3.2. 根据id查询评论用户 和评论目标用户
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            List<UserDTO> targetUsers = userClient.queryUserByIds(targetUserIds);;
            Map<Long, UserDTO> userMap = new HashMap<>(users.size());
            Map<Long, UserDTO> targetUserMap = new HashMap<>(targetUsers.size());
            if (CollUtils.isNotEmpty(users)) {
                userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
                targetUserMap = targetUsers.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
            }

            // 4. 封装VO
            List<ReplyVO> voList = new ArrayList<>(records.size());
            for (InteractionReply q : records) {
                // 4.1. 将PO转VO，属性拷贝
                ReplyVO vo = BeanUtils.copyBean(q, ReplyVO.class);
                voList.add(vo);

                // 4.2. 用户信息
                UserDTO user = userMap.get(q.getUserId());
                UserDTO targetUser = targetUserMap.get(q.getTargetUserId());
                if (user != null) {
                    //判断评论者是否匿名评论
                    if(vo.getAnonymity() == false) {
                        vo.setUserName(user.getName());
                        vo.setUserIcon(user.getIcon());
                    }else{
                        vo.setUserName(null);
                        vo.setUserIcon(null);
                    }
                    //TODO: 评论目标用户信息是否匿名评论
                    vo.setTargetUserName(targetUser.getName());
                }
            }
            return PageDTO.of(pageResult, voList);
        }

    }

    @Override
    public void updateReplyHidden(Long id, Boolean hidden) {
        InteractionReply reply = getById(id);
        if(reply == null){
            throw new IllegalArgumentException("互动问题回答或评论不存在");
        }
        reply.setHidden(hidden);
        updateById(reply);
    }
}
