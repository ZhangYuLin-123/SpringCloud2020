package com.zyl.springcloud.service.impl;

import com.zyl.springcloud.service.PaymentHystrixService;
import org.springframework.stereotype.Component;

@Component
public class PaymentFallbackServiceImpl implements PaymentHystrixService {
    @Override
    public String paymentInfoOK(Integer id) {
        return "PaymentFallbackService fall back-paymentInfo_OK ,o(╥﹏╥)o";
    }

    @Override
    public String paymentInfoTimeOut(Integer id) {
        return "PaymentFallbackService fall back-paymentInfo_TimeOut ,o(╥﹏╥)o";
    }

}
