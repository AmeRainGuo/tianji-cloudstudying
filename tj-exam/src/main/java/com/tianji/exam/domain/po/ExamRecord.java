package com.tianji.exam.domain.po;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试记录
 * 对应MongoDB集合：exam_records
 * 这是整个考试模块最核心的表
 */
@Data
@Document(collection = "exam_records")
public class ExamRecord {

    @Id
    private String id; // 考试记录ID，就是接口返回给前端的那个id

    /**
     * 用户ID
     */
    @Field("user_id")
    private Long userId;

    /**
     * 业务ID（小节ID/试卷ID）
     */
    @Field("biz_id")
    private Long bizId;

    /**
     * 考试类型：1-练习 2-正式考试
     */
    @Field("type")
    private Integer type;

    /**
     * 本次考试的所有题目ID列表
     */
    @Field("question_ids")
    private List<Long> questionIds;

    /**
     * 用户提交的答案（key:题目ID, value:用户答案）
     */
    @Field("user_answers")
    private List<String> userAnswers;

    /**
     * 考试状态：0-进行中 1-已提交 2-已过期
     */
    @Field("status")
    private Integer status;

    /**
     * 总分
     */
    @Field("total_score")
    private Integer totalScore;

    /**
     * 用户得分
     */
    @Field("score")
    private Integer score;

    /**
     * 开始时间
     */
    @Field("start_time")
    private LocalDateTime startTime;

    /**
     * 提交时间
     */
    @Field("submit_time")
    private LocalDateTime submitTime;

}