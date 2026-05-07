package com.tianji.exam.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


import java.util.List;

@Data
@ApiModel(description = "考试开始响应")
public class ExamTestVO {

    @ApiModelProperty("考试记录id")
    private String id;

    @ApiModelProperty("题目列表")
    private List<QuestionVO> questions;

}
