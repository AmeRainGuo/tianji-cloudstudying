package com.tianji.learning.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 笔记采集记录表
 * </p>
 *
 * @author Amerain
 * @since 2026-05-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("note_gather")
public class NoteGather implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 被采集的原笔记ID
     */
    private Long noteId;

    /**
     * 采集人(当前用户)ID
     */
    private Long gatherUserId;

    /**
     * 采集时间
     */
    private LocalDateTime gatherTime;


}
