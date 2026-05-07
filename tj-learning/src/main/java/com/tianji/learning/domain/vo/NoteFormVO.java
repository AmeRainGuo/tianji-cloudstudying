package com.tianji.learning.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@ApiModel("笔记表单信息")
public class NoteFormVO {

    @ApiModelProperty("笔记ID")
    private Long id;

    @ApiModelProperty("笔记内容")
    private String content;

    @ApiModelProperty("是否私密")
    private Boolean isPrivate;

    @ApiModelProperty("记录笔记时的视频播放时间点(单位:秒)")
    private Integer noteMoment;

    @ApiModelProperty("是否是采集的笔记")
    private Boolean isGathered; // 是否是采集的笔记

    @ApiModelProperty("作者用户ID")
    private Long authorId; //如果是采集的 携带作者用户ID

    @ApiModelProperty("作者用户名")
    private String authorName; // 作者用户名

    @ApiModelProperty("作者头像URL")
    private String authorIcon; // 作者头像URL

    @ApiModelProperty("笔记创建时间")
    private String createTime; // 笔记创建时间

}

