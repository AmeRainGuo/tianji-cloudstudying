package com.tianji.learning.service;

import cn.hutool.db.PageResult;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionReply;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;

/**
 * <p>
 * 互动问题的回答或评论 服务类
 * </p>
 *
 * @author Amerain
 * @since 2026-04-23
 */
public interface IInteractionReplyService extends IService<InteractionReply> {

    void addReply(ReplyDTO replyDTO);

    PageDTO<ReplyVO> queryReplyList(ReplyPageQuery pageQuery);

    PageDTO<ReplyVO> queryAdminReplyList(ReplyPageQuery pageQuery);

    void updateReplyHidden(Long id, Boolean hidden);
}
