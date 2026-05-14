package com.tianji.exam.repository;

import com.tianji.exam.domain.po.ExamQuestion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ExamQuestionRepository extends MongoRepository<ExamQuestion, String> {

}