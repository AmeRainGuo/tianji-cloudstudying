package com.tianji.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.OrderCouponDTO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.strategy.discount.Discount;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface IDiscountService  {


    List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourseDTOs);

    CouponDiscountDTO queryDiscountDetailByOrder(OrderCouponDTO orderCouponDTO);
}
