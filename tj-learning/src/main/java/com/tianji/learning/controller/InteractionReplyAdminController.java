package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动问题的回答或评论 前端控制器
 * </p>
 *
 * @author Amerain
 * @since 2026-04-23
 */
@RestController
@RequestMapping("/admin/replies")
@RequiredArgsConstructor
@Api(tags = "互动问答的回答或评论的相关接口")
public class InteractionReplyAdminController {

    private final IInteractionReplyService replyService;

    @ApiOperation("管理端分页查询互动问题回答或评论")
    @GetMapping("/page")
    public PageDTO<ReplyVO> queryAdminReplyList(@RequestParam ReplyPageQuery pageQuery) {

        return replyService.queryAdminReplyList(pageQuery);
    }

    @ApiOperation("管理端隐藏或显示互动问题回答或评论")
    @PostMapping("/{id}/hidden/{hidden}")
    public void updateReplyHidden(@PathVariable Long id, @PathVariable Boolean hidden) {
        replyService.updateReplyHidden(id, hidden);
    }
}
