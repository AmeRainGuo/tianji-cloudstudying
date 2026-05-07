package com.tianji.promotion.handler;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.utils.CollUtils;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.service.ICouponService;
import com.tianji.promotion.service.IUserCouponService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Component
@RequiredArgsConstructor
public class CouponJobHandler {

    private final ICouponService couponService;

    private final IUserCouponService userCouponService;

    /**
     * 定时开始发放优惠券
     */
    @XxlJob("startDistributeCouponJob")
    public void startDistributeCoupon() {
        LocalDateTime now = LocalDateTime.now();
        int index = XxlJobHelper.getShardIndex();
        int total = XxlJobHelper.getShardTotal();
            // MP 链式条件构造（零SQL）
        boolean isSuccess = couponService.lambdaUpdate()
                .eq(Coupon::getStatus, CouponStatus.UN_ISSUE.getValue())
                .le(Coupon::getIssueBeginTime, now)
                .apply("id % {0} = {1}", total, index)
                .set(Coupon::getStatus, CouponStatus.ISSUING.getValue())
                .update();

        XxlJobHelper.log("开始发放任务执行完成，更新成功：{}", isSuccess);
    }

    /**
     * 定时结束发放优惠券
     */
    @XxlJob("stopDistributeCouponJob")
    public void stopDistributeCoupon() {
        LocalDateTime now = LocalDateTime.now();
        int index = XxlJobHelper.getShardIndex();
        int total = XxlJobHelper.getShardTotal();
        // MP 链式条件构造（零SQL）
        boolean isSuccess = couponService.lambdaUpdate()
                .eq(Coupon::getStatus, CouponStatus.ISSUING.getValue())
                .le(Coupon::getIssueEndTime, now)
                .apply("id % {0} = {1}", total, index)
                .set(Coupon::getStatus, CouponStatus.FINISHED.getValue())
                .update();

        XxlJobHelper.log("结束发放任务执行完成，更新成功：{}", isSuccess);
    }

    /**
     * 定时提醒使用优惠券
     */
    @XxlJob("remindUseCouponJob")
    public void remindUseCoupon() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowEnd = LocalDate.now().plusDays(1).atTime(23, 59, 59);

        int pageNo = 1;
        int pageSize = 500;
        while (true) {
            Page<UserCoupon> page = new Page<>(pageNo, pageSize);

            List<UserCoupon> list = userCouponService.lambdaQuery()
                    .eq(UserCoupon::getStatus, 1) // 未使用
                    .ge(UserCoupon::getTermEndTime, todayStart) // 大于等于今天0点
                    .le(UserCoupon::getTermEndTime, tomorrowEnd)// 小于等于明天24点 这么设计是为了避免临近00点时，优惠券过期时间恰好是00:00:00的情况，导致提醒失败
                    .list();

            if (CollUtils.isEmpty(list)) {
                break;
            }

// TODO    未完成信息发送功能        // 4. 批量发送提醒
//            for (UserCoupon uc : list) {
//                messageService.sendCouponExpireRemind(
//                        uc.getUserId(),
//                        uc.getCouponId(),
//                        uc.getTermEndTime()
//                );
//            }

            pageNo++;
        }
    }
}

