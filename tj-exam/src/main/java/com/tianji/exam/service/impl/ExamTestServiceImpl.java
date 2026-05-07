package com.tianji.exam.service.impl;

import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.learning.LearningClient;
import com.tianji.api.dto.course.*;
import com.tianji.api.dto.exam.QuestionBizDTO;
import com.tianji.api.dto.exam.QuestionDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.exam.domain.dto.StartExamDTO;
import com.tianji.exam.domain.dto.SubmitExamDTO;
import com.tianji.exam.domain.po.ExamRecord;
import com.tianji.exam.domain.po.QuestionDetail;
import com.tianji.exam.domain.vo.*;
import com.tianji.exam.mq.message.QuestionTimesMessage;
import com.tianji.exam.repository.ExamRecordRepository;
import com.tianji.exam.service.ExamTestService;
import com.tianji.exam.service.IQuestionBizService;
import com.tianji.exam.service.IQuestionDetailService;
import com.tianji.exam.service.IQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class ExamTestServiceImpl implements ExamTestService {

    private static final int EXAM_DURATION_MINUTES = 90; // 正式考试时长90分钟

    private static final int Max_Question_Number= 10;

    private final LearningClient learningClient;

    private final IQuestionService questionServiceImpl;

    private final IQuestionBizService questionBizService;

    private final MongoTemplate mongoTemplate;

    private final IQuestionDetailService questionDetailService;

    private final RabbitMqHelper rabbitMqHelper;

    private final CourseClient courseClient;

    private final CatalogueClient catalogueClient;


    @Override
    public ExamTestVO startExam(StartExamDTO startExamDTO) {
        //获取当前用户id
        Long userId = UserContext.getUser();

        //创建返回对象内部的考试记录对象
        ExamTestVO examTestVO = new ExamTestVO();

        //查询用户购买的课程的情况
        Long lessonValid = learningClient.isLessonValid(startExamDTO.getCourseId());
        if (lessonValid  == null) {
            throw new IllegalArgumentException("用户未购买课程，请先购买课程");
        }

        //查询考试记录
        Query query = Query.query(Criteria.where("user_id").is(userId))
                .addCriteria(Criteria.where("biz_id").is(startExamDTO.getSectionId()))
                .addCriteria(Criteria.where("type").is(startExamDTO.getType()));

        ExamRecord record = mongoTemplate.findOne(query, ExamRecord.class);

        //如果存在
        if (record != null){
            //判断是否已提交或过期
            if (record.getStatus() != 0){
                throw new IllegalArgumentException("你已完成该测试，继续学习下一节吧！");
            }

            //进行中的情况下 判断是否是考试
            if(record.getType()==2){
                //考试 判断是否超时
                LocalDateTime endTime = record.getStartTime().plusMinutes(EXAM_DURATION_MINUTES);
                if (LocalDateTime.now().isAfter(endTime)) {
                    // 超时了！当场执行自动提交
                    log.info("用户[{}]进入考试时发现考试[{}]已超时，当场自动提交", userId, record.getId());
                    //自动提交并判定0分
                    SubmitExamDTO submitExamDTO = new SubmitExamDTO();
                    submitExamDTO.setId(record.getId());
                    submitExamDTO.setExamDetails(new ArrayList<>());
                    submitExam(submitExamDTO);

                    throw new IllegalArgumentException("考试已超时，已自动提交");
                }
                //没超时 返回考试信息继续考试
            }
            //为练习或考试未超时 返回练习信息继续练习
            //获取题目id列表
            List<Long> questionIds = record.getQuestionIds();
            //获取题目列表信息
            List<QuestionDTO> questionDTOS = questionServiceImpl.queryQuestionByIds(questionIds);
            //封装到考试信息对象
            List<QuestionVO> questionVOS = BeanUtils.copyList(questionDTOS, QuestionVO.class);

            //返回考试信息继续考试
            examTestVO.setId(record.getId());
            examTestVO.setQuestions(questionVOS);

            return examTestVO;
        }
        //没有记录 则未开始 返回试题信息并新增考试或测试记录
        //获取试题信息
        List<QuestionBizDTO> questionBizDTOS = questionBizService.queryQuestionIdsByBizId(startExamDTO.getSectionId());
        if (CollectionUtils.isEmpty(questionBizDTOS)) {
            throw new IllegalArgumentException("该小节暂无题目");
        }

        List<Long> questionIds = questionBizDTOS.stream().map(QuestionBizDTO::getQuestionId)
                .collect(Collectors.toList());

        //判断是否是练习
        if (startExamDTO.getType()==1){
            //如果是练习
            //随机从题库id中抽取一定数量的题
            questionIds = randomSelectQuestions(questionIds, Max_Question_Number);
        }

        //获取题目列表信息
        List<QuestionDTO> questionDTOS = questionServiceImpl.queryQuestionByIds(questionIds);
        //封装到考试信息对象
        List<QuestionVO> questionVOS = BeanUtils.copyList(questionDTOS, QuestionVO.class);

        //算出总分
        int totalScore = questionDTOS.stream()
                .mapToInt(QuestionDTO::getScore)  // 把每道题的分值抽出来
                .sum();                          // 直接求和 = 试卷满分


        //新增考试记录
        ExamRecord newRecord = new ExamRecord();
        newRecord.setUserId(userId);
        newRecord.setBizId(startExamDTO.getSectionId());
        newRecord.setType(startExamDTO.getType());
        newRecord.setQuestionIds(questionIds);
        newRecord.setStatus(0); // 进行中
        newRecord.setStartTime(LocalDateTime.now());
        newRecord.setTotalScore(totalScore);
        //更新获取到存储后的考试记录id
        mongoTemplate.insert(newRecord);

        //返回试题信息
        examTestVO.setId(newRecord.getId());
        examTestVO.setQuestions(questionVOS);
        return examTestVO;
    }


    /**
     * 随机抽取题目
     * @param questionIds 所有题目ID列表
     * @param count 要抽取的数量
     * @return 随机抽取后的题目ID列表
     */
    private List<Long> randomSelectQuestions(List<Long> questionIds, int count) {
        if (CollectionUtils.isEmpty(questionIds)) {
            return CollUtils.emptyList();
        }

        // 如果题目数量小于等于要抽取的数量，直接返回全部
        if (questionIds.size() <= count) {
            return new ArrayList<>(questionIds);
        }

        // 创建新列表打乱，不修改原列表
        List<Long> shuffledList = new ArrayList<>(questionIds);
        Collections.shuffle(shuffledList);

        // 取前count个
        return shuffledList.subList(0, count);
    }


    @Override
    public void submitExam(SubmitExamDTO submitExamDTO) {
        //查询考试记录
        Query recordQuery = Query.query(Criteria.where("_id").is(submitExamDTO.getId()));
        ExamRecord examRecord = mongoTemplate.findById(recordQuery, ExamRecord.class);

        if (examRecord == null) {
            throw new IllegalArgumentException("考试记录不存在");
        }

        //获取当前用户
        Long userId = UserContext.getUser();
        if (!examRecord.getUserId().equals(userId)) {
            throw new IllegalArgumentException("考试记录不属于当前用户");
        }

        //校验考试状态
        if (!examRecord.getStatus().equals(0)) {
            throw new IllegalArgumentException("考试已结束或您已提交");
        }

        //分别遍历出题目id集合，用户回答集合
        Map<Long, String> userAnswerMap = submitExamDTO.getExamDetails().stream()
                .collect(Collectors.toMap(
                        SubmitExamDTO.ExamDetails::getId, // 题目ID
                        SubmitExamDTO.ExamDetails::getUserAnswer, // 用户答案
                        (oldValue, newValue) -> newValue // 重复题目取最后一个
                ));

        //获取数据库中对应考试存储的题目id列表
        List<Long> questionIds = examRecord.getQuestionIds();

        //批阅
        //获取正确答案列表和分值列表
        //获取分值类别
        List<QuestionDTO> questionDTOS = questionServiceImpl.queryQuestionByIds(questionIds);
        //获取正确答案列表
        List<QuestionDetail> questionDetails = questionDetailService.lambdaQuery()
                .in(QuestionDetail::getId, questionIds)
                .list();
        //分别封装到Map集合中 用id当key查找
        Map<Long, Integer> scoreMap = questionDTOS.stream()
                .collect(Collectors.toMap(QuestionDTO::getId, QuestionDTO::getScore));
        Map<Long, String> correctAnswerMap = questionDetails.stream()
                .collect(Collectors.toMap(QuestionDetail::getId, QuestionDetail::getAnswer));

        //批改
        int userScore = 0;
        List<String> finalUserAnswers = new ArrayList<>(); // 按数据库题目顺序整理后的答案列表

        List<Long> rightQuestionIds = new ArrayList<>();
        for (Long questionId : questionIds) {
            // 获取用户答案（没答的题存空字符串）
            String userAnswer = userAnswerMap.getOrDefault(questionId, "");
            finalUserAnswers.add(userAnswer);

            // 获取题目分值和正确答案
            Integer score = scoreMap.get(questionId);
            String correctAnswer = correctAnswerMap.get(questionId);

            // 题目不存在，跳过
            if (score == null || correctAnswer == null) {
                log.warn("题目[{}]不存在，跳过判分", questionId);
                continue;
            }

            // 比对答案，答对加分
            if (isAnswerCorrect(userAnswer, correctAnswer)) {
                userScore += score;
                rightQuestionIds.add(questionId);
            }
        }

        //mongodb原子性更新 不用再去set record了
//        examRecord.setUserAnswers(finalUserAnswers);
//        examRecord.setStatus(1);
//        examRecord.setSubmitTime(LocalDateTime.now());
//        examRecord.setScore(userScore);

        //原子更新：只有状态为0时才更新
        Query query = Query.query(Criteria.where("_id").is(examRecord.getId()));
        query.addCriteria(Criteria.where("status").is(0));

        Update update = new Update();
        update.set("status", 1);
        update.set("score", userScore);
        update.set("user_answers", finalUserAnswers);
        update.set("submit_time", LocalDateTime.now());

        mongoTemplate.updateFirst(query, update, ExamRecord.class);

        //使用mq发送加分操作
        rabbitMqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.LEARN_SECTION,
                userId);

        //统计回答次数 正确次数
        QuestionTimesMessage questionTimesMessage = new QuestionTimesMessage();
        questionTimesMessage.setQuestionIds(questionIds);
        questionTimesMessage.setRightQuestionIds(rightQuestionIds);

        rabbitMqHelper.send(MqConstants.Exchange.EXAM_EXCHANGE,
                MqConstants.Key.UPDATE_QUESTION_KEY,
                questionTimesMessage);
    }

    /**
     * 判断答案是否正确（支持单选题、多选题、判断题）
     */
    private boolean isAnswerCorrect(String userAnswer, String correctAnswer) {
        // 空答案直接判错
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }
        // 正确答案为空，直接判对（防止脏数据）
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
            return true;
        }

        // 分割成集合，排序后对比 (利用set排序 防止前端多选顺序与后端不一样但答案整体一样时判错）
        Set<String> userSet = new HashSet<>(Arrays.asList(userAnswer.split(",")));
        Set<String> correctSet = new HashSet<>(Arrays.asList(correctAnswer.split(",")));

        return userSet.equals(correctSet);
    }


//    /**
//     * 判分提交试卷操作
//     */
//    public void autoSubmit(ExamRecord record) {
//        //提交试卷
//        SubmitExamDTO submitExamDTO = new SubmitExamDTO();
//        //获取题目id列表
//        List<Long> questionIds = record.getQuestionIds();
//        //获取用户回答列表
//        List<String> userAnswers = record.getUserAnswers();
//        // 安全校验：题目和答案数量必须一致
//        if (questionIds.size() != userAnswers.size()) {
//            log.error("考试[{}]题目数量({})与答案数量({})不匹配，按空答案提交",
//                    record.getId(), questionIds.size(), userAnswers.size());
//        }
//
//        //获取题目列表信息
//        List<QuestionDTO> questionDTOS = questionServiceImpl.queryQuestionByIds(questionIds);
//        Map<Long, Integer> questionTypeMap = questionDTOS.stream().collect(Collectors.toMap(QuestionDTO::getId, QuestionDTO::getType));
//
//        Map<Long, String> userAnswerMap = new HashMap<>();
//        for (int i = 0; i < record.getUserAnswers().size(); i++) {
//            Long questionId = questionIds.get(i);
//            String userAnswer = record.getUserAnswers().get(i);
//            userAnswerMap.put(questionId, userAnswer);
//        }
//
//        List<SubmitExamDTO.ExamDetails> details = questionIds.stream().map(qId -> {
//            SubmitExamDTO.ExamDetails detail = new SubmitExamDTO.ExamDetails();
//            detail.setId(qId);
//            detail.setQuestionType(questionTypeMap.get(qId));
//            detail.setUserAnswer(userAnswerMap.get(qId));
//            return detail;
//        }).collect(Collectors.toList());
//
//        submitExamDTO.setId(record.getId());
//        submitExamDTO.setExamDetails(details);
//        submitExam(submitExamDTO);
//        // 抛出异常，拒绝继续考试
//        log.info("用户[{}]考试[{}]自动提交完成", record.getUserId(), record.getId());
//        throw new IllegalArgumentException("考试已超时，已自动提交");
//    }


    @Override
    public PageDTO<ExamRecordPageVO> pageQueryExam(PageQuery pageQuery) {
        Long userId = UserContext.getUser();

        // 1. 构建分页条件
        int pageNo = pageQuery.getPageNo();
        int pageSize = pageQuery.getPageSize();
        int skip = (pageNo - 1) * pageSize;


        // 2. 构建查询条件：当前用户 + 已提交的考试记录
        Query queryWrapper = Query.query(Criteria.where("user_id").is(userId))
                .addCriteria(Criteria.where("status").is(1)) // 只查已提交的
                .skip(skip)
                .limit(pageSize)
                .with(Sort.by(Sort.Direction.DESC, "submit_time")); // 按提交时间倒序

        List<ExamRecord> records = mongoTemplate.find(queryWrapper, ExamRecord.class);

        // 4. 查询总条数（用于分页计算）
        Query countQuery = Query.query(Criteria.where("user_id").is(userId))
                .addCriteria(Criteria.where("status").is(1));
        long total = mongoTemplate.count(countQuery, ExamRecord.class);
        long pages = (long) Math.ceil( (double) total / pageSize );

        //获取课程和小节信息
        List<Long> sectionIds = records.stream().map(ExamRecord::getBizId).collect(Collectors.toList());
        List<CourseBase> courseBaseList = catalogueClient.batchQuerySection(sectionIds);
        Map<Long, CourseBase> sectionIdCourseBaseMap = courseBaseList.stream().collect(Collectors.toMap(CourseBase::getSectionId, v -> v));


        // 5. 封装VO
        List<ExamRecordPageVO> voList = new ArrayList<>();
        for (ExamRecord record : records) {
            ExamRecordPageVO vo = new ExamRecordPageVO();
            BeanUtils.copyProperties(record, vo);
            vo.setCommitTime(record.getSubmitTime());
            // 计算考试用时：(提交时间 - 开始时间) 转成分钟 最多90分钟
            long minutes = Duration.between(record.getStartTime(), record.getSubmitTime()).toMinutes();
            vo.setDuration((int) (minutes>=90 ? 90 : minutes));

            CourseBase section = sectionIdCourseBaseMap.get(record.getBizId());
            if (section != null) {
                vo.setCourseName(section.getName());
                vo.setSectionName(section.getSectionName());
            } else {
                vo.setCourseName("未知课程");
                vo.setSectionName("未知小节");
            }

            voList.add(vo);
        }

        // 直接 new 分页对象，按它的字段赋值
        PageDTO<ExamRecordPageVO> pageDTO = new PageDTO<>();
        pageDTO.setTotal(total);       // 总条数
        pageDTO.setPages(pages);       // 总页数
        pageDTO.setList(voList);       // 当前页数据列表

        return pageDTO;
    }

    @Override
    public List<ExamQuestionDetailVO> queryExamDetailRecord(String id) {
        Long userId = UserContext.getUser();

        // 1. 查询考试记录，校验权限和状态
        ExamRecord record = mongoTemplate.findById(id, ExamRecord.class);
        if (record == null) {
            throw new IllegalArgumentException("考试记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权查看他人的考试记录");
        }
        if (record.getStatus() != 1) {
            throw new IllegalArgumentException("考试未提交，无法查看详情");
        }

        // 2. 获取题目列表和用户答案
        List<Long> questionIds = record.getQuestionIds();
        List<String> userAnswers = record.getUserAnswers();

        // 3. 批量查询题目详情
        List<QuestionDTO> questionDTOS = questionServiceImpl.queryQuestionByIds(questionIds);
        Map<Long, QuestionDTO> questionMap = questionDTOS.stream()
                .collect(Collectors.toMap(QuestionDTO::getId, q -> q));

        // 4. 封装每道题的详情
        List<ExamQuestionDetailVO> detailList = new ArrayList<>();
        for (int i = 0; i < questionIds.size(); i++) {
            Long questionId = questionIds.get(i);
            String userAnswer = userAnswers.get(i);
            QuestionDTO questionDTO = questionMap.get(questionId);

            if (questionDTO == null) {
                log.warn("题目[{}]不存在，跳过", questionId);
                continue;
            }

            ExamQuestionDetailVO vo = new ExamQuestionDetailVO();
            vo.setAnswer(userAnswer);
            vo.setComment("无");

            // 判断是否正确
            boolean isCorrect = isAnswerCorrect(userAnswer, questionDTO.getAnswer());
            vo.setCorrect(isCorrect);

            // 本题得分
            vo.setScore(isCorrect ? questionDTO.getScore() : 0);

            //适配你的内部类，封装题目详情
            QuestionExamDetailVO questionVO = new QuestionExamDetailVO();
            questionVO.setId(String.valueOf(questionDTO.getId()));
            questionVO.setName(questionDTO.getName());
            questionVO.setType(questionDTO.getType());
            questionVO.setScore(questionDTO.getScore());
            questionVO.setAnswer(questionDTO.getAnswer());
            questionVO.setAnalysis(questionDTO.getAnalysis());
            questionVO.setDifficulty(questionDTO.getDifficulty());

            vo.setQuestion(questionVO);
            detailList.add(vo);
        }

        return detailList;
    }
}
