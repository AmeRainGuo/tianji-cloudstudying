package com.tianji.exam.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.exam.domain.dto.StartExamDTO;
import com.tianji.exam.domain.dto.SubmitExamDTO;
import com.tianji.exam.domain.vo.ExamQuestionDetailVO;
import com.tianji.exam.domain.vo.ExamRecordPageVO;
import com.tianji.exam.domain.vo.ExamTestVO;
import com.tianji.exam.service.ExamTestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "考试管理相关接口")
@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
public class ExamTestController {

    private final ExamTestService examTestService;

    @ApiOperation("获取试题并开始考试接口")
    @PostMapping
    public ExamTestVO startExam(@RequestParam StartExamDTO startExamDTO){
        return examTestService.startExam(startExamDTO);
    }

    @ApiOperation("提交考试记录接口")
    @PostMapping("/details")
    public void submitExam(@RequestParam SubmitExamDTO submitExamDTO){
        examTestService.submitExam(submitExamDTO);
    }

    @ApiOperation("分页查询考试记录接口")
    @GetMapping("/page")
    public PageDTO<ExamRecordPageVO> pageQueryExam(PageQuery pageQuery){
        return examTestService.pageQueryExam(pageQuery);
    }

    @ApiOperation("查询考试记录详情接口")
    @GetMapping
    public List<ExamQuestionDetailVO> queryExamDetailRecord(@RequestParam String id){
        return examTestService.queryExamDetailRecord(id);
    }
}
