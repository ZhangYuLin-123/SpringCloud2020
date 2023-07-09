package com.zyl.springcloud.controller;

import com.zyl.springcloud.entities.CommonResult;
import com.zyl.springcloud.entities.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@RestController
@RequestMapping("consumer")
public class OrderController {

    // zookeeper服务中心的服务名称
    public static final String INVOKE_URL = "http://cloud-provider-payment";  // 注意大小写问题

    @Resource
    private RestTemplate restTemplate;

    @GetMapping(value = "/payment/zk")
    public String paymentInfo() {
        String result = restTemplate.getForObject(INVOKE_URL + "/payment/zk", String.class);
        return result;
    }
}
