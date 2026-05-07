package com.tianji.exam.service;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.exam.controller.ExamTestController;
import com.tianji.exam.domain.dto.StartExamDTO;
import com.tianji.exam.domain.dto.SubmitExamDTO;
import com.tianji.exam.domain.po.ExamRecord;
import com.tianji.exam.domain.vo.ExamQuestionDetailVO;
import com.tianji.exam.domain.vo.ExamRecordPageVO;
import com.tianji.exam.domain.vo.ExamTestVO;

import java.util.List;

public interface ExamTestService {

    /**
     * 获取试题并开始考试接口
     * @param startExamDTO
     */
    ExamTestVO startExam(StartExamDTO startExamDTO);

    /**
     * 提交考试记录接口
     * @param submitExamDTO
     */
    void submitExam(SubmitExamDTO submitExamDTO);


    PageDTO<ExamRecordPageVO> pageQueryExam(PageQuery pageQuery);

    List<ExamQuestionDetailVO> queryExamDetailRecord(String id);
}
