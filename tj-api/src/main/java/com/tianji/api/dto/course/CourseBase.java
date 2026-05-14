package com.tianji.api.dto.course;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@ApiModel(value="课程基础信息")
@AllArgsConstructor
@NoArgsConstructor
public class CourseBase {

    @ApiModelProperty("业务id")
    private Long sectionId;

    @ApiModelProperty("业务名称")
    private String sectionName;

    @ApiModelProperty("课程id")
    private Long id;

    @ApiModelProperty("课程名称")
    private String name;
}
