package com.tianji.exam.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(description = "考试记录分页数据")
public class ExamRecordPageVO {

    @ApiModelProperty("考试id")
    private String id;

    @ApiModelProperty("考试类型（考试、练习）")
    private Integer type;

    @ApiModelProperty("考试得分")
    private Integer score;

    @ApiModelProperty("提交时间")
    private LocalDateTime commitTime;

    @ApiModelProperty("考试用时")
    private Integer duration;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("小节名称")
    private String sectionName;
}
