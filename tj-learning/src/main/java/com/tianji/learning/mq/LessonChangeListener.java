package com.tianji.learning.mq;


import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;

@Component
@Slf4j
@RequiredArgsConstructor
public class LessonChangeListener {

    private final ILearningLessonService lessonService;

    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = "learning.lesson.pay.queue",durable="true"),
            exchange = @Exchange(name=MqConstants.Exchange.ORDER_EXCHANGE,type= ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_PAY_KEY
    ))
    public void listenLessonPay(OrderBasicDTO order){
        // 1.健壮性处理
        if(order == null || order.getUserId() == null|| CollUtils.isEmpty(order.getCourseIds())){
            //数据异常
            log.error("接收到的消息异常，订单数据为空");
            return;
        }
        // 2.添加课程
        log.debug("添加课程，用户id：{}，订单id：{}，课程id集合：{}",order.getUserId(),order.getOrderId(),order.getCourseIds());
        lessonService.addUserLessons(order.getUserId(), order.getCourseIds());
    }

    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = "learning.lesson.refund.queue",durable="true"),
            exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE,type= ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_REFUND_KEY
    ))
    public void listenLessonRefund(OrderBasicDTO order) {
        // 1.健壮性处理
        if(order == null || order.getUserId() == null|| CollUtils.isEmpty(order.getCourseIds())){
            //数据异常
            log.error("接收到的消息异常，订单数据为空");
            return;
        }
        // 2.删除课程
        log.debug("删除课程，用户id：{}，订单id：{}，课程id集合：{}",order.getUserId(),order.getOrderId(),order.getCourseIds());
        lessonService.removeCourseByRefund(order);
    }
}
