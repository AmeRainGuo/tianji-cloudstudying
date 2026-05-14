package com.tianji.promotion.constants;

import com.tianji.common.enums.BaseEnum;
import lombok.Getter;

@Getter
public enum DiscountType implements BaseEnum {
    // 纯枚举定义，无任何语法错误
    PER_PRICE_DISCOUNT(1, "每满减"),
    RATE_DISCOUNT(2, "折扣"),
    NO_THRESHOLD(3, "无门槛"),
    PRICE_DISCOUNT(4, "满减");

    private final int value;
    private final String desc;

    DiscountType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static DiscountType of(Integer value) {
        if (value == null) return null;
        for (DiscountType type : values()) {
            if (type.value == value) return type;
        }
        return null;
    }
}