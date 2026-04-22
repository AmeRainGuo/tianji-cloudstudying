package com.tianji.learning.mapper;

import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.po.LearningRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学习记录表 Mapper 接口
 * </p>
 *
 * @author Amerain
 * @since 2026-04-20
 */
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {


    List<IdAndNumDTO> countLearnedSections(@Param("userId") Long userId, @Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);
}
