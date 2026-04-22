package com.tianji.learning.controller;


import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学习记录表 前端控制器
 *
 * @author Amerain
 * @since 2026-04-20
 */
@RestController
@RequestMapping("/learning-records")
@Api(tags = "学习记录接口")
@RequiredArgsConstructor
public class LearningRecordController {

    private final ILearningRecordService learningRecordService;

    @ApiOperation("查询当前用户指定课程的学习进度")
    @GetMapping("/course/{courseId}")
    public LearningLessonDTO queryLearningRecordByCourse(@ApiParam( value = "指定课程id",example = "2") @PathVariable("courseId") Long courseId){
        return learningRecordService.queryLearningRecordByCourseId(courseId);
    }

    @PostMapping
    @ApiOperation("提交学习记录")
    public void addLearningRecord(@RequestBody LearningRecordFormDTO fromDTO){
        learningRecordService.addLearningRecord(fromDTO);
    }
}
