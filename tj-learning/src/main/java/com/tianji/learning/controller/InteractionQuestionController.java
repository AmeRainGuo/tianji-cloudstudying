package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author Amerain
 * @since 2026-04-23
 */
@RestController
@RequestMapping("/questions")
@Api(tags = "互动问答的相关接口")
@RequiredArgsConstructor
public class InteractionQuestionController {

    private final IInteractionQuestionService questionService;

    @PostMapping
    @ApiOperation("新增互动问题")
    public void saveQuestion(@RequestBody QuestionFormDTO questionFormDTO) {
        questionService.saveQuestion(questionFormDTO);
    }

    @PutMapping("/{id}")
    @ApiOperation("修改互动问题")
    public void updateQuestion(@PathVariable Long id, @RequestBody QuestionFormDTO questionFormDTO) {
        questionService.updateQuestion(id, questionFormDTO);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除互动问题")
    public void deleteQuestion(@PathVariable Long id) {
        questionService.removeQuestion(id);
    }

    @ApiOperation("获取互动问题列表")
    @GetMapping("/page")
    public PageDTO<QuestionVO> queryQuestionByPage(QuestionPageQuery pageQuery) {
        return questionService.queryQuestionPage(pageQuery);
    }

    @ApiOperation("根据id查询互动问题")
    @GetMapping("/{id}")
    public QuestionVO queryQuestionById(@PathVariable Long id) {
        return questionService.queryQuestionById(id);
    }
}
