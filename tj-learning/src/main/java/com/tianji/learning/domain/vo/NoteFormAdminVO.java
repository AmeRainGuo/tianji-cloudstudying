package com.tianji.learning.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("笔记表单 分页查询管理员")
public class NoteFormAdminVO {
    @ApiModelProperty("笔记ID")
    private Long id;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("章名称")
    private String chapterName;

    @ApiModelProperty("节名称")
    private String sectionName;

    @ApiModelProperty("笔记内容")
    private String content;

    @ApiModelProperty("是否被隐藏")
    private Boolean hidden;

    @ApiModelProperty("被采集次数")
    private Long usedTimes;

    @ApiModelProperty("作者名称")
    private String authorName;

    @ApiModelProperty("笔记创建时间")
    private String createTime;
}
