package com.zyl.springcloud.controller;

import com.zyl.springcloud.entities.CommonResult;
import com.zyl.springcloud.entities.Payment;
import com.zyl.springcloud.service.OrderFeignService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/consumer")
public class OrderFeignController {

    @Resource
    private OrderFeignService orderFeignService;

    @GetMapping("/feign/payment/{id}")
    public CommonResult<Payment> getPaymentById(@PathVariable("id")Long id){
        return orderFeignService.getPaymentById(id);
    }

    // 测试timeout
    @GetMapping("/feign/payment/timeout")
    public String testTimeout(){
        // OpenFeign客户端一般默认等待1秒钟
        return orderFeignService.testTimeout();
    }

}
