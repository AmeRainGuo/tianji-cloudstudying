package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
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
@Transactional
@Slf4j
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {

    private final CourseClient courseClient;

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
}
