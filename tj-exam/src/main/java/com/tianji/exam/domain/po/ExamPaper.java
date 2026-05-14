package com.tianji.exam.domain.po;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * 试卷信息
 * 对应MongoDB集合：exam_papers
 */
@Data
@Document(collection = "exam_papers")
public class ExamPaper {

    @Id
    private String id;

    /**
     * 试卷名称
     */
    @Field("title")
    private String title;

    /**
     * 试卷总分
     */
    @Field("total_score")
    private Integer totalScore;

    /**
     * 考试时长（分钟）
     */
    @Field("duration")
    private Integer duration;

    /**
     * 试卷状态：0-草稿 1-已发布 2-已下架
     */
    @Field("status")
    private Integer status;

    /**
     * 创建时间
     */
    @Field("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Field("update_time")
    private LocalDateTime updateTime;
}