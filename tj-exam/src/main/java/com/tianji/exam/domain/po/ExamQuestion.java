package com.tianji.exam.domain.po;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试题目
 * 对应MongoDB集合：exam_questions
 */
@Data
@Document(collection = "exam_questions")
public class ExamQuestion {

    @Id
    private String id;

    /**
     * 所属试卷ID
     */
    @Field("paper_id")
    private Long paperId;

    /**
     * 题干
     */
    @Field("title")
    private String title;

    /**
     * 选项列表（JSON数组格式）
     */
    @Field("options")
    private List<String> options;

    /**
     * 正确答案
     */
    @Field("answer")
    private String answer;

    /**
     * 题目类型：1-单选题 2-多选题 3-判断题
     */
    @Field("type")
    private Integer type;

    /**
     * 题目分值
     */
    @Field("score")
    private Integer score;

    /**
     * 题目解析
     */
    @Field("analyze")
    private String analyze;

    /**
     * 排序
     */
    @Field("sort")
    private Integer sort;

    /**
     * 创建时间
     */
    @Field("create_time")
    private LocalDateTime createTime;
}