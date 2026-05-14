package com.tianji.learning.controller;


import cn.hutool.db.PageResult;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionQuestionService;
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
@RequestMapping("/replies")
@RequiredArgsConstructor
@Api(tags = "互动问答的回答或评论的相关接口")
public class InteractionReplyController {

    private final IInteractionReplyService replyService;

    @ApiOperation("新增互动问题回答或评论")
    @PostMapping
    public void addReply(@RequestBody ReplyDTO replyDTO) {

        replyService.addReply(replyDTO);
    }

    @ApiOperation("分页查询互动问题回答或评论")
    @GetMapping("/page")
    public PageDTO<ReplyVO> queryReplyList(@RequestParam ReplyPageQuery pageQuery) {

        return replyService.queryReplyList(pageQuery);
    }
}
