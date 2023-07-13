package com.zyl.springcloud.service;

import com.zyl.springcloud.entities.CommonResult;
import com.zyl.springcloud.entities.Payment;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Component
@FeignClient(value = "CLOUD-PAYMENT-SERVICE")
public interface OrderFeignService {

    @GetMapping(value = "/payment/{id}")
    CommonResult<Payment> getPaymentById(@PathVariable("id")Long id);

    /*测试ribbon超时*/
    @GetMapping("/payment/feign/timeout")
    String testTimeout();
}
