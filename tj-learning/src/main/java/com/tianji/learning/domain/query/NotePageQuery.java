package com.tianji.learning.domain.query;

import com.tianji.common.domain.query.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(description = "学习笔记分页查询条件")
public class NotePageQuery extends PageQuery {

    @ApiModelProperty("课程ID")
    private Long courseId;

    @ApiModelProperty("节ID")
    private Long sectionId;

    @ApiModelProperty("是否只查询我的笔记")
    @NotNull(message = "是否只查询我的笔记不能为空")
    private Boolean onlyMine;
}
