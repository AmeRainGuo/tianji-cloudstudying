package com.tianji.promotion.constants;

public interface PromotionConstants {
    String COUPON_CODE_SERIAL_KEY = "coupon:code:serial";
    String COUPON_CODE_MAP_KEY = "coupon:code:map";
    String COUPON_CACHE_KEY_PREFIX = "prs:coupon:";
    String USER_COUPON_CACHE_KEY_PREFIX = "prs:user:coupon:";
    String COUPON_RANGE_KEY = "coupon:range:";
    String[] RECEIVE_COUPON_ERROR_MSG = {"优惠券不存在", "优惠券库存不足", "优惠券发放已经结束或尚未开始" , "超出领取数量"};
    String[] EXCHANGE_COUPON_ERROR_MSG = {"兑换码已兑换", "没有可用的优惠券", "优惠券不存在", "兑换码已过期", "兑换码已经被使用"};
}
