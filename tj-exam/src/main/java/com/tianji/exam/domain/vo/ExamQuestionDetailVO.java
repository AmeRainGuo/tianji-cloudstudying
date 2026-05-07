package com.tianji.exam.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "考试题目详情")
public class ExamQuestionDetailVO {

    @ApiModelProperty("学员答案（用逗号拼接，比如 \"1,2,3\"）")
    private String answer;

    @ApiModelProperty("老师评语（这里先写死，后续扩展）")
    private String comment = "无";

    @ApiModelProperty("是否答对")
    private Boolean correct;

    @ApiModelProperty("本题得分")
    private Integer score;

    @ApiModelProperty("题目详情")
    private QuestionExamDetailVO question;

}