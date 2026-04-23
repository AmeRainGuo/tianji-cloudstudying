package com.tianji.learning.utils;

import com.alibaba.fastjson.JSON;
import com.tianji.common.utils.JsonUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.DelayQueue;

@Slf4j
@RequiredArgsConstructor
@Component
public class LearningRecordDelayTaskHandler {

    private final StringRedisTemplate redisTemplate;

    private final DelayQueue<DelayTask<RecordTaskData>> queue = new DelayQueue<>();

    private final static String RECORD_KEY_TEMPLATE = "learning:record:{}";

    private final LearningRecordMapper learningRecordMapper;

    private final ILearningLessonService learningLessonService;

    private static volatile boolean begin = true;
    @PostConstruct
    public void init(){
        CompletableFuture.runAsync(this::handleDelayTask);
    }

    @PreDestroy
    public void destroy(){
        begin = false;
        log.info("学习记录延迟任务处理线程已销毁");
    }

    public void handleDelayTask() {
        while (begin) {
            try {
                //获取到启动延迟任务
                DelayTask<RecordTaskData> task = queue.take();
                RecordTaskData data = task.getData();
                //查询redis缓存
                LearningRecord record = readRecordCache(data.getLessonId(), data.getSectionId());
                if (record == null) {
                    continue;//缓存未命中 直接进行下次循环
                }
                //比较数据 moment值
                if (!Objects.equals(record.getMoment(), data.getMoment())){
                //不一致，说明用户还在持续提交播放进度，放弃旧数据
                    continue;
                }
                //一致，则持久化播放进度数据到数据库
                //更新学习记录的moment值
                record.setFinished(null);
                learningRecordMapper.updateById(record);
                //跟新课表最近学习信息
                LearningLesson lesson = new LearningLesson();
                lesson.setId(record.getLessonId());
                lesson.setLatestSectionId(record.getSectionId());
                lesson.setLatestLearnTime(LocalDateTime.now());
                learningLessonService.updateById(lesson);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void addLearningRecordTask(LearningRecord record){
        //添加数据到redis缓存
        writeRecordCache(record);
        //提交延迟任务到延迟队列DelayQueue
        queue.add(new DelayTask<>(new RecordTaskData(record), Duration.ofSeconds(20)));
    }

    public void writeRecordCache(LearningRecord record) {
        log.debug("写入学习记录的缓存数据，record={}", record);
        try {
            //数据转换
            RecordCacheData cacheData = new RecordCacheData(record);
            String json = JSON.toJSONString(cacheData);
            //写入redis
            String key = StringUtils.format(RECORD_KEY_TEMPLATE, record.getLessonId());
            redisTemplate.opsForHash().put(key, record.getSectionId().toString(), json);
            //添加缓存
            redisTemplate.expire(key, Duration.ofMinutes(1));
        } catch (Exception e) {
            log.error("写入学习记录的缓存数据失败，record={}", record, e);
        }

    }

    public LearningRecord readRecordCache(Long lessonId, Long sectionId) {
        try {
            //读取redis数据
            String key = StringUtils.format(RECORD_KEY_TEMPLATE, lessonId);
            Object cacheData = redisTemplate.opsForHash().get(key, sectionId.toString());
            if (cacheData == null) {
                return null;
            }
            //数据的检测和转化
            RecordCacheData recordCacheData = JsonUtils.toBean(cacheData.toString(), RecordCacheData.class);
            LearningRecord record = new LearningRecord();
            record.setId(recordCacheData.getId());
            record.setMoment(recordCacheData.getMoment());
            record.setFinished(recordCacheData.getFinished());
            return record;
        } catch (Exception e) {
            log.error("缓存读取异常",e);
            return null;
        }
    }

    public void cleanRecordCache(Long lessonId, Long sectionId) {
        //删除数据
        String key = StringUtils.format(RECORD_KEY_TEMPLATE, lessonId);
        redisTemplate.opsForHash().delete(key, sectionId.toString());
    }

    @Data
    @NoArgsConstructor
    private static class RecordCacheData{
        private Long id;
        private Integer moment;
        private Boolean finished;

        public RecordCacheData(LearningRecord record){
            this.id = record.getId();
            this.moment = record.getMoment();
            this.finished = record.getFinished();
        }
    }

    @Data
    @NoArgsConstructor
    private static class RecordTaskData{
        private Long lessonId;
        private Long sectionId;
        private Integer moment;

        public RecordTaskData(LearningRecord record) {
            this.lessonId = record.getLessonId();
            this.sectionId = record.getSectionId();
            this.moment = record.getMoment();
        }
    }
}
