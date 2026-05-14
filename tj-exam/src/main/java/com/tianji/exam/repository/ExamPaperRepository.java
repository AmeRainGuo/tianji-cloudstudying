package com.tianji.exam.repository;

import com.tianji.exam.domain.po.ExamPaper;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExamPaperRepository extends MongoRepository<ExamPaper, String> {

}