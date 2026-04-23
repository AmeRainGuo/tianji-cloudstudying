package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
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
@RequestMapping("/admin/questions")
@Api(tags = "管理员端互动问答的相关接口")
@RequiredArgsConstructor
public class InteractionQuestionAdminController {

    private final IInteractionQuestionService questionService;

    @ApiOperation("获取管理员端互动问题列表")
    @GetMapping("/page")
    public PageDTO<QuestionAdminVO> queryQuestionByPageAdmin(QuestionAdminPageQuery query) {
        return questionService.queryQuestionPageAdmin(query);
    }

    @ApiOperation("管理员端隐藏/显示互动问题")
    @PutMapping("/{id}/hidden/{hidden}")
    public void updateQuestionHidden(@PathVariable Long id, @PathVariable Boolean hidden) {
        questionService.updateQuestionHidden(id, hidden);
    }

    @ApiOperation("管理员端查看互动问题详情")
    @GetMapping("/{id}")
    public QuestionAdminVO queryQuestionById(@PathVariable Long id) {
        return questionService.queryQuestionAdminById(id);
    }
}
