package com.zyl.springcloud.controller;

import com.zyl.springcloud.entities.CommonResult;
import com.zyl.springcloud.entities.Payment;
import com.zyl.springcloud.lb.MyLoadBalancer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.net.URI;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/consumer")
public class OrderController {

    // 重点是这里，改成提供者在Eureka上的名称，而且无需写端口号
    // 取决于我们在提供者配置应用的name，比如：CLOUD-PAYMENY-SERVICE
    // 同时要注意使用@LoadBalanced注解赋予RestTemplate负载均衡能力
    public static final String PAYMENT_URL = "http://CLOUD-PAYMENT-SERVICE";

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private MyLoadBalancer myLoadBalancer;

    @Resource
    private DiscoveryClient discoveryClient;

    @PostMapping("/payment/create")
    public CommonResult<Payment> create (Payment payment){
        return restTemplate.postForObject(PAYMENT_URL + "/payment/create",//请求地址
                payment,//请求参数
                CommonResult.class);//返回类型
    }

    @GetMapping("/payment/{id}")
    public CommonResult<Payment> getPaymentById(@PathVariable("id")Long id){
        return restTemplate.getForObject(PAYMENT_URL + "/payment/" + id,//请求地址
                CommonResult.class);//返回类型
    }

    @GetMapping("/payment/getForEntity/{id}")
    public CommonResult<Payment> getPayment2(@PathVariable("id") Long id) {
        ResponseEntity<CommonResult> entity = restTemplate.getForEntity(PAYMENT_URL + "/payment/" + id, CommonResult.class);

        if (entity.getStatusCode().is2xxSuccessful()) {
            return entity.getBody();
        } else {
            return new CommonResult<>(444, "操作失败");
        }
    }



    // 测试自定义的负载均衡规则
    // 注意:
    //     测试时需要取消restTemplate的@LoadBalanced注解！！！     否则报错：No instances available for [your_ip]
    //     需要取消启动类上的@RibbonClient注解
    @GetMapping(value = "/payment/lb")
    public String getPaymentLB() {
        List<ServiceInstance> instances = discoveryClient.getInstances("CLOUD-PAYMENT-SERVICE");

        if (instances == null || instances.isEmpty()) {
            return null;
        }

        // 调用自定义的负载均衡策略
        ServiceInstance serviceInstance = myLoadBalancer.instances(instances);
        URI uri = serviceInstance.getUri();
        String url = uri + "/payment/lb";
        log.info("url:" + url);
        return restTemplate.getForObject(url, String.class);

    }
}

