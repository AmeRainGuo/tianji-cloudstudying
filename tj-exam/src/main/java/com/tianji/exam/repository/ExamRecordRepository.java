package com.tianji.exam.repository;


import com.tianji.exam.domain.po.ExamRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import javax.validation.constraints.NotNull;

public interface ExamRecordRepository extends MongoRepository<ExamRecord, String> {


}