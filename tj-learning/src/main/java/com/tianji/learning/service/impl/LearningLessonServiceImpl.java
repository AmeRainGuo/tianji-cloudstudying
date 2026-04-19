package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonStatusVO;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.swagger.models.auth.In;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author Amerain
 * @since 2026-04-18
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {

    private final CourseClient courseClient;

    private final CatalogueClient catalogueClient;

    @Transactional
    @Override
    public void addUserLessons(Long userId, List<Long> courseIds) {
        //查询课程有效期
        List<CourseSimpleInfoDTO> courseInfoList = courseClient.getSimpleInfoList(courseIds);
        if(CollUtils.isEmpty(courseInfoList)){
            //课程不存在，直接返回
            log.error("课程不存在，无法添加到课表");
            return;
        }
        //循环遍历，处理
        List<LearningLesson> lessonList = new ArrayList<>(courseInfoList.size());
        for (CourseSimpleInfoDTO courseInfo : courseInfoList) {
            LearningLesson lesson = new LearningLesson();
            //获取课程有效期
            Integer validDuration = courseInfo.getValidDuration();

            //获取当前时间
            if (validDuration != null && validDuration > 0) {
                LocalDateTime now = LocalDateTime.now();

                lesson.setCreateTime(now);
                lesson.setExpireTime(now.plusMonths(validDuration));
            }
            //填充user Id和course Id
            lesson.setUserId(userId);
            lesson.setCourseId(courseInfo.getId());
            lessonList.add(lesson);
        }
        //批量新增
        saveBatch(lessonList);
    }

    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        //获取当前登录用户id
        Long userId = UserContext.getUser();
        //分页查询
        Page<LearningLesson> page = lambdaQuery().eq(LearningLesson::getUserId, userId).page(query.toMpPage("latest_learn_time", false));
        //查询课程信息
        List<LearningLesson> records = page.getRecords();
        if(CollUtils.isEmpty(records)){
            //没有课程，直接返回
            log.info("没有课程");
            return PageDTO.empty(page);
        }

        Set<Long> courseIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());

        //查询课程有效期信息
        List<CourseSimpleInfoDTO> courseInfoList = courseClient.getSimpleInfoList(courseIds);
        if(CollUtils.isEmpty(courseInfoList)){
            //课程不存在，直接返回
            log.error("课程不存在，无法查询课表");
            throw new BadRequestException("课程不存在!");
        }
        //把课程集合处理成Map，key是courseid，值是course本身
        Map<Long, CourseSimpleInfoDTO> courseMap = courseInfoList.stream()
                .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, v -> v));

        //循环遍历，处理
        List<LearningLessonVO> voList = new ArrayList<>(records.size());
        for (LearningLesson lesson : records) {
            //填充课程信息 用beanutils拷贝
            LearningLessonVO vo = new LearningLessonVO();
            vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);

            //获取课程信息，填充到vo
            CourseSimpleInfoDTO courseInfo = courseMap.get(lesson.getCourseId());
            vo.setCourseName(courseInfo.getName());
            vo.setCourseCoverUrl(courseInfo.getCoverUrl());
            vo.setSections(courseInfo.getSectionNum());
            voList.add(vo);
        }
        //填充分页信息
        return PageDTO.of(page, voList);
    }

    // 查询当前正在学习的课程
    @Override
    public LearningLessonVO queryMyCurrentLesson() {
        // 1.获取当前登录的用户
        Long userId = UserContext.getUser();
        // 2.查询正在学习的课程 select * from xx where user_id = #{userId} AND status = 1 order by latest_learn_time limit 1
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING.getValue())
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1")
                .one();
        if (lesson == null) {
            return null;
        }
        // 3.拷贝PO基础属性到VO
        LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
        // 4.查询课程信息
        CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        if (cInfo == null) {
            throw new BadRequestException("课程不存在");
        }
        vo.setCourseName(cInfo.getName());
        vo.setCourseCoverUrl(cInfo.getCoverUrl());
        vo.setSections(cInfo.getSectionNum());
        // 5.统计课表中的课程数量 select count(1) from xxx where user_id = #{userId}
        Long courseAmount = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .count();
        vo.setCourseAmount(Math.toIntExact(courseAmount));
        // 6.查询小节信息
        List<CataSimpleInfoDTO> cataInfos =
                catalogueClient.batchQueryCatalogue(CollUtils.singletonList(lesson.getLatestSectionId()));
        if (!CollUtils.isEmpty(cataInfos)) {
            CataSimpleInfoDTO cataInfo = cataInfos.get(0);
            vo.setLatestSectionName(cataInfo.getName());
            vo.setLatestSectionIndex(cataInfo.getCIndex());
        }
        return vo;
    }

    // 用户删除学习课程
    @Override
    public void removeCourseByCourseId(Long courseId) {
        // 1.获取当前登录的用户
        Long userId = UserContext.getUser();

        // 更规范的做法：不物理删除，而是标记为已失效
        lambdaUpdate()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .set(LearningLesson::getStatus, LessonStatus.INVALID.getValue())
                .update();
    }

    // 退款删除学习课程
    @Override
    public void removeCourseByRefund(OrderBasicDTO orderBasicDTO) {
        // 1.获取当前登录的用户
        Long userId = orderBasicDTO.getUserId();
        // 更规范的做法：不物理删除，而是标记为已失效
        lambdaUpdate()
                .eq(LearningLesson::getUserId, userId)
                .in(LearningLesson::getCourseId, orderBasicDTO.getCourseIds())
                .set(LearningLesson::getStatus, LessonStatus.INVALID.getValue())
                .update();
    }

    @Override
    public Long isLessonValid(Long courseId) {
        // 1.获取当前登录的用户
        Long userId = UserContext.getUser();
        // 2.校验当前用户是否可以学习这门课程
        LearningLesson learningLesson = lambdaQuery().eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        if (learningLesson == null) {
            log.info("用户没有此课程");
            return null;
        }
        if (learningLesson.getStatus() == LessonStatus.EXPIRED.getValue()) {
            log.info("课程已过期");
            return null;
        }
        log.info("课程有效");
        return learningLesson.getId();
    }

    @Override
    public LearningLessonStatusVO queryLearningLessonByCourseId(Long courseId) {
        // 1.获取当前登录的用户
        Long userId = UserContext.getUser();
        //查询用户课程表中指定课程状态
        LearningLesson learningLesson = lambdaQuery().eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        if (learningLesson == null) {
            log.info("用户没有此课程");
            return null;
        }

        //用户有这个课程
        //用户已购买 返回用户有效期 学习进度
        if (learningLesson.getStatus() == LessonStatus.LEARNING.getValue()) {
            //配置返回给前端的数据
            LearningLessonStatusVO vo = BeanUtils.copyBean(learningLesson, LearningLessonStatusVO.class);
            return vo;
        }

        //用户未购买 返回课程信息
        CourseFullInfoDTO courseInfo = courseClient.getCourseInfoById(courseId, false, false);
        if (courseInfo == null) {
            throw new BadRequestException("课程不存在");
        }
        LearningLessonStatusVO vo = new LearningLessonStatusVO();
        vo.setCourseId(courseId);
        vo.setPrice(courseInfo.getPrice());
        vo.setValidDuration(courseInfo.getValidDuration());
        vo.setSectionNum(courseInfo.getSectionNum());
        vo.setPurchaseEndTime(courseInfo.getPurchaseEndTime());
        return vo;
    }

    @Override
    public Integer queryLearnedCount(Long courseId) {
        // 1.查询课程的学习人数
        Long learnedCount = lambdaQuery().eq(LearningLesson::getCourseId, courseId)
                .count();
        return Math.toIntExact(learnedCount);
    }
}
