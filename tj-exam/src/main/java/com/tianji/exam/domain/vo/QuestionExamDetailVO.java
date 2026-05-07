package com.tianji.exam.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

    @Data
    @ApiModel(description = "题目详情")
    public class QuestionExamDetailVO {
        @ApiModelProperty("问题id")
        private String id;

        @ApiModelProperty("问题名称")
        private String name;

        @ApiModelProperty("问题类型")
        private Integer type;

        @ApiModelProperty("问题分值")
        private Integer score;

        @ApiModelProperty("正确答案")
        private String answer;

        @ApiModelProperty("答案解析")
        private String analysis;

        @ApiModelProperty("难度")
        private Integer difficulty;
    }