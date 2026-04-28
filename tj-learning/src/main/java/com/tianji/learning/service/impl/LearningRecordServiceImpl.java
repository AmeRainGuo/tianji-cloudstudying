package com.tianji.learning.service.impl;

import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.utils.LearningRecordDelayTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author Amerain
 * @since 2026-04-20
 */
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {

    private final ILearningLessonService learningLessonService;

    private final CourseClient courseClient;

    private final LearningRecordDelayTaskHandler delayTaskHandler;

    private final RabbitMqHelper rabbitMqHelper;
    @Override
    public LearningLessonDTO queryLearningRecordByCourseId(Long courseId) {
        //获取登录游湖
        Long userId = UserContext.getUser();
        //查询课表
        LearningLesson learningLesson = learningLessonService.queryByUserIdAndCourseId(userId, courseId);
        if (learningLesson == null) {
            return null;
        }
        //查询学习记录
        List<LearningRecord> learningRecords = lambdaQuery()
                .eq(LearningRecord::getLessonId, learningLesson.getId())
                .list();
        List<LearningRecordDTO> records = BeanUtils.copyList(learningRecords, LearningRecordDTO.class);
        //封装结果
        LearningLessonDTO learningLessonDTO = new LearningLessonDTO();
        learningLessonDTO.setId(learningLesson.getId());
        learningLessonDTO.setLatestSectionId(learningLesson.getLatestSectionId());
        learningLessonDTO.setRecords(records);
        return learningLessonDTO;
    }

    @Override
    @Transactional
    public void addLearningRecord(LearningRecordFormDTO recordDTO) {
        // 获取登录用户
        Long userId = UserContext.getUser();
        // 处理学习记录
        boolean finished = false;
        if (recordDTO.getSectionType() == SectionType.VIDEO) {
            // 1. 处理视频
            finished = handleVideoRecord(userId, recordDTO);
        }else {
            // 2. 处理考试
            finished = handleExamRecord(userId, recordDTO);
        }
        if(!finished){
            //没有新的完成小节，无需处理课表数据
            return;
        }
        // 处理课表数据
        handleLearningLessonChanges(recordDTO);
    }

    private void handleLearningLessonChanges(LearningRecordFormDTO recordDTO) {
        //查询课表
        LearningLesson learningLesson = learningLessonService.getById(recordDTO.getLessonId());
        if(learningLesson == null){
            throw new DbException("课表不存在");
        }
        //判断是否有新的完成小节
        boolean allFinished = false;
        //如果有新完成的小节查询课程
        CourseFullInfoDTO courseInfo = courseClient.getCourseInfoById(learningLesson.getCourseId(), false, false);
        if(courseInfo == null){
            throw new DbException("课程不存在");
        }
        //比较是否全部学完
        allFinished = learningLesson.getLearnedSections() + 1 >= courseInfo.getSectionNum();

        //更新课表
        learningLessonService.lambdaUpdate()
                .set(learningLesson.getLearnedSections() == 0, LearningLesson::getStatus, LessonStatus.LEARNING.getValue())//课表未开始学习 设置为已学习
                .set(allFinished, LearningLesson::getStatus, LessonStatus.FINISHED.getValue())//课表全部完成 设置为已学完
                .setSql("learned_sections = learned_sections + 1")//已学习小节数量增加1
                .set(LearningLesson::getLatestSectionId, recordDTO.getSectionId())//最近一次学习的小节id
                .set(LearningLesson::getLatestLearnTime, LocalDateTime.now())//最近一次学习的时间
                .eq(LearningLesson::getId, learningLesson.getId())//根据id查询
                .update();
    }

    private boolean handleVideoRecord(Long userId, LearningRecordFormDTO recordDTO) {
        //查询旧的学习记录
        LearningRecord oldRecord = queryOldRecord(recordDTO.getLessonId(), recordDTO.getSectionId());
        //判断是否存在
        if(oldRecord == null){
            //不存在，则新增
            //1.转换DTO为PO
            LearningRecord record = BeanUtils.copyBean(recordDTO, LearningRecord.class);
            //2.填充数据
            record.setUserId(userId);
            record.setFinished(false);
            //写入数据库
            boolean result = save(record);
            if(!result){
                throw new DbException("新增学习记录失败");
            }
            return true;
        }
        //存在，则更新
        //4.1判断是否是第一次提交
        boolean finished = recordDTO.getMoment() *2>= recordDTO.getDuration() && !oldRecord.getFinished();
        if(!finished){
            LearningRecord record = new LearningRecord();
            record.setId(oldRecord.getId());
            record.setFinished(oldRecord.getFinished());
            record.setSectionId(oldRecord.getSectionId());
            record.setLessonId(oldRecord.getLessonId());
            record.setMoment(recordDTO.getMoment());
            delayTaskHandler.addLearningRecordTask(record);
            return false;
        }
        //4.2更新进度
        boolean success = lambdaUpdate()
                .set(LearningRecord::getMoment, recordDTO.getMoment())
                .set(finished, LearningRecord::getFinished, true)
                .set(finished, LearningRecord::getFinishTime, recordDTO.getCommitTime())
                .eq(LearningRecord::getId, oldRecord.getId())
                .update();
        if(!success){
            throw new DbException("更新学习记录失败");
        }
        rabbitMqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.LEARN_SECTION,
                userId);
        //4.3删除缓存
        delayTaskHandler.cleanRecordCache(recordDTO.getLessonId(), recordDTO.getSectionId());
        return finished;
    }

    private LearningRecord queryOldRecord(Long lessonId, Long sectionId) {
        //查询缓存
        LearningRecord record = delayTaskHandler.readRecordCache(lessonId, sectionId);
        //如果命中 直接返回
        if(record != null){
            return record;
        }

        //未命中
        record = lambdaQuery()
                .eq(LearningRecord::getLessonId, lessonId)
                .eq(LearningRecord::getSectionId, sectionId)
                .one();
        if(record == null){
            return null;
        }
        //写入缓存
        delayTaskHandler.writeRecordCache(record);
        return record;
    }

    private boolean handleExamRecord(Long userId, LearningRecordFormDTO recordDTO) {
        //1.转换DTO为PO
        LearningRecord record = BeanUtils.copyBean(recordDTO, LearningRecord.class);
        //2.填充数据
        record.setUserId(userId);
        record.setMoment(recordDTO.getMoment());
        record.setFinished(true);
        record.setFinishTime(LocalDateTime.now());
        //写入数据库
        boolean result = save(record);
        if(!result){
           throw new DbException("保存学习记录失败");
        }

        rabbitMqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.LEARN_SECTION,
                userId);
        return true;
    }
}
