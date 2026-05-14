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
 * 学习笔记主表
 * </p>
 *
 * @author Amerain
 * @since 2026-05-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("note")
public class Note implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 笔记主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 笔记内容
     */
    private String content;

    /**
     * 是否私密 0-公开 1-私密
     */
    private Boolean isPrivate;

    /**
     * 记录笔记时的视频播放时间点(单位:秒)
     */
    private Integer noteMoment;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 章ID
     */
    private Long chapterId;

    /**
     * 节ID
     */
    private Long sectionId;

    /**
     * 作者用户ID
     */
    private Long authorId;

    /**
     * 管理端是否隐藏 0-显示 1-隐藏
     */
    private Boolean hidden;

    /**
     * 被采集(引用)次数
     */
    private Integer usedTimes;

    /**
     * 创建/发布时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
