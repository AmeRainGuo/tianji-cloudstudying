package com.tianji.course.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
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
