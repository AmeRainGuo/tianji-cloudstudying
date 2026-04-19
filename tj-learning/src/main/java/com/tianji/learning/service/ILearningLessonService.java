package com.tianji.learning.service;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonStatusVO;
import com.tianji.learning.domain.vo.LearningLessonVO;

import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author Amerain
 * @since 2026-04-18
 */
public interface ILearningLessonService extends IService<LearningLesson> {

    // 添加课程
    void addUserLessons(Long userId, List<Long> courseIds);

    // 分页查询我的课程表
    PageDTO<LearningLessonVO> queryMyLessons(PageQuery query);

    // 查询当前正在学习的课程
    LearningLessonVO queryMyCurrentLesson();

    // 用户删除学习课程
    void removeCourseByCourseId(Long courseId);

    // 退款删除学习课程
    void removeCourseByRefund(OrderBasicDTO orderBasicDTO);

    // 校验当前用户是否可以学习当前课程
    Long isLessonValid(Long courseId);

    // 查询用户课表中指定课程状态
    LearningLessonStatusVO queryLearningLessonByCourseId(Long courseId);

    // 查询课程的学习人数
    Integer queryLearnedCount(Long courseId);
}
