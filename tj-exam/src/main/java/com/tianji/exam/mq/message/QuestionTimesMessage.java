package com.tianji.exam.mq.message;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description = "更新题目正确次数和答题次数所用消息")
public class QuestionTimesMessage {

    @ApiModelProperty("全部题目id")
    private List<Long> questionIds;

    @ApiModelProperty("正确题目id")
    private List<Long> rightQuestionIds;
}
