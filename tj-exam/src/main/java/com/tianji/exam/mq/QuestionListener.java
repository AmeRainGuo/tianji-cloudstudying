package com.tianji.exam.mq;

import com.tianji.common.utils.CollUtils;
import com.tianji.exam.domain.po.Question;
import com.tianji.exam.mq.message.QuestionTimesMessage;
import com.tianji.exam.service.IQuestionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


import java.util.List;

import static com.tianji.common.constants.MqConstants.Exchange.EXAM_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.UPDATE_QUESTION_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionListener {

    private final IQuestionService questionService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "question.update.queue", durable = "true"),
            exchange = @Exchange(name = EXAM_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = UPDATE_QUESTION_KEY
            ))
    public void listenUpdateQuestion(QuestionTimesMessage questionTimesMessage){
        log.debug("监听到更新习题相关的消息");

        List<Long> questionIds = questionTimesMessage.getQuestionIds();
        if (!CollUtils.isEmpty(questionIds)) {
            questionService.lambdaUpdate()
                    .setSql("answer_times = answer_times + 1")
                    .in(Question::getId, questionIds)
                    .update();
        }
        List<Long> rightQuestionIds = questionTimesMessage.getRightQuestionIds();
        if (!CollUtils.isEmpty(rightQuestionIds)) {
            questionService.lambdaUpdate()
                    .setSql("correct_times = correct_times + 1")
                    .in(Question::getId, rightQuestionIds)
                    .update();
        }

    }
}
