package com.tianji.learning.domain.query;

import com.tianji.common.domain.query.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(description = "学习笔记分页查询条件")
public class NotePageAdminQuery extends PageQuery {

    @ApiModelProperty("课程名称关键字")
    private String name ;

    @ApiModelProperty("笔记状态，是否在用户端隐藏")
    private Boolean hidden;

    @ApiModelProperty("更新时间区间的开始时间")
    private LocalDateTime beginTime;

    @ApiModelProperty("更新时间区间的结束时间")
    private LocalDateTime endTime;

}
