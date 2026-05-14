package com.tianji.api.dto.course;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@ApiModel(description = "目录简单信息")
@AllArgsConstructor
@NoArgsConstructor
public class CataNoteVO {

    @ApiModelProperty("课程id")
    private Long courseId;
    @ApiModelProperty("目录id")
    private Long id;
    @ApiModelProperty("目录名称")
    private String name;


}
