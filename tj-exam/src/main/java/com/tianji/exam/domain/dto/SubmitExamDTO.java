package com.tianji.exam.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.w3c.dom.stylesheets.LinkStyle;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@ApiModel(description = "开始考试实体")
public class SubmitExamDTO {

     @ApiModelProperty("考试记录id")
     @NotNull(message = "考试记录id不能为空")
     private String id;

     @ApiModelProperty("考试记录详情列表")
     @NotNull(message = "考试记录详情不能为空")
     private List<ExamDetails> examDetails;

     @Data
     @ApiModel(description = "提交考试记录详情")
     public static class ExamDetails {

          @ApiModelProperty("题目id")
          private Long id;

          @ApiModelProperty("题目类型，1：单选题，2：多选题，3：不定向选择题，4：判断题，5：主观题")
          private Integer questionType;

          @ApiModelProperty("用户答案")
          private String userAnswer;

     }

}
