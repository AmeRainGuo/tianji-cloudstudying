package com.tianji.learning.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@ApiModel("笔记表单信息")
public class NoteFormDTO {


    @ApiModelProperty("笔记内容")
    @NotNull(message = "笔记内容不能为空")
    @Size(max = 1000, message = "笔记内容长度不能超过1000个字符")
    private String content;

    @ApiModelProperty("是否私密")
    @NotNull(message = "是否私密不能为空")
    private Boolean isPrivate;

    @ApiModelProperty("记录笔记时的视频播放时间点(单位:秒)")
    @NotNull(message = "记录笔记时的视频播放时间点不能为空")
    private Integer noteMoment;

    @ApiModelProperty("课程ID")
    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @ApiModelProperty("章ID")
    @NotNull(message = "章ID不能为空")
    private Long chapterId;


    @ApiModelProperty("节ID")
    @NotNull(message = "节ID不能为空")
    private Long sectionId;

}

