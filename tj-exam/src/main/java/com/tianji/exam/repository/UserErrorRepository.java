package com.tianji.exam.repository;

import com.tianji.exam.domain.po.UserError;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface UserErrorRepository extends MongoRepository<UserError, String> {

}