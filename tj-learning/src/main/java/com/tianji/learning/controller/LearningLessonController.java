package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonStatusVO;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 学生课程表 前端控制器
 * </p>
 *
 * @author Amerain
 * @since 2026-04-18
 */
@RestController
@RequestMapping("/lessons")
@Api(tags = "我的课程表接口")
@RequiredArgsConstructor
public class LearningLessonController {

    private final ILearningLessonService learningLessonService;

    @GetMapping("/page")
    @ApiOperation("分页查询我的课程表")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {

        PageDTO<LearningLessonVO> result = learningLessonService.queryMyLessons(query);
        return result;
    }

    @GetMapping("/now")
    @ApiOperation("查询我正在学习的课程")
    public LearningLessonVO queryMyCurrentLesson() {
        return learningLessonService.queryMyCurrentLesson();
    }

    @DeleteMapping("/now/{courseId}")
    @ApiOperation("用户删除学习课程")
    public void deleteMyLesson(@PathVariable Long courseId) {
        learningLessonService.removeCourseByCourseId(courseId);
    }


    @GetMapping("/{courseId}/valid")
    @ApiOperation("校验当前用户是否可以学习当前课程")
    public Long isLessonValid(@PathVariable Long courseId) {
        // 校验当前用户是否可以学习这门课程的逻辑
        return learningLessonService.isLessonValid(courseId);
    }

    @GetMapping("/lessons/{courseId}")
    @ApiOperation("查询用户课表中指定课程状态")
    public LearningLessonStatusVO queryLearningLessonByCourseId(@PathVariable Long courseId) {
        return learningLessonService.queryLearningLessonByCourseId(courseId);
    }

    @GetMapping("/{courseId}/count")
    @ApiOperation("查询课程的学习人数")
    public Integer queryLearnedCount(@PathVariable Long courseId) {
        return learningLessonService.queryLearnedCount(courseId);
    }

    @PostMapping("/plans")
    @ApiOperation("创建学习计划")
    public void createLearningPlan(@Valid @RequestBody LearningPlanDTO planDTO) {
        learningLessonService.createLearningPlan(planDTO.getCourseId(), planDTO.getFreq());
    }

    @GetMapping("/plans")
    @ApiOperation("查询我的学习计划")
    public LearningPlanPageVO queryMyPlans(PageQuery  query) {
        return learningLessonService.queryMyPlans(query);
    }
}
