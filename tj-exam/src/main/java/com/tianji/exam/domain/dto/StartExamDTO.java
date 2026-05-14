package com.tianji.exam.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel(description = "开始考试表单实体")
public class StartExamDTO {

     // 课程id
     @ApiModelProperty("课程id")
     @NotNull(message = "课程id不能为空")
     private Long courseId;

     // 小节id
     @ApiModelProperty("小节id")
     @NotNull(message = "小节id不能为空")
     private Long sectionId;

     // 类型，1-练习，2-考试
     @ApiModelProperty("类型，1-练习，2-考试")
     @NotNull(message = "类型不能为空")
     private Integer type;
}
