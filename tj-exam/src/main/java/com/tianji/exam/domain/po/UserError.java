package com.tianji.exam.domain.po;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * 用户错题记录
 * 对应MongoDB集合：user_errors
 */
@Data
@Document(collection = "user_errors") // 明确指定集合名称
public class UserError {

    /**
     * MongoDB自动生成的主键ID
     */
    @Id
    private String id;

    /**
     * 用户ID
     */
    @Field("user_id") // 数据库字段名用下划线，Java用驼峰，自动映射
    private Long userId;

    /**
     * 题目ID
     */
    @Field("question_id")
    private Long questionId;

    /**
     * 试卷ID（关联exam_papers）
     */
    @Field("paper_id")
    private Long paperId;

    /**
     * 题目类型：1-单选题 2-多选题 3-判断题
     */
    @Field("question_type")
    private Integer questionType;

    /**
     * 用户提交的答案
     */
    @Field("user_answer")
    private String userAnswer;

    /**
     * 正确答案
     */
    @Field("correct_answer")
    private String correctAnswer;

    /**
     * 是否答对
     */
    @Field("is_correct")
    private Boolean isCorrect;

    /**
     * 答题时间
     */
    @Field("create_time")
    private LocalDateTime createTime;
}