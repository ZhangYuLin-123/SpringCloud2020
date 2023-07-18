package com.zyl.springcloud.service;

import cn.hutool.core.util.IdUtil;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PaymentService {
    /**
     * 正常访问
     * @param id
     * @return
     */
    public String paymentinfo_Ok(Integer id){
        return "线程池：" + Thread.currentThread().getName() + "--paymentInfo_OK，id:" + id;
    }

    /**
     * 超时访问，设置自身调用超时的峰值，峰值内正常运行，超过了峰值需要服务降级 自动调用fallbackMethod 指定的方法
     * 超时异常或者运行异常 都会进行服务降级
     * @param id
     * @return
     */
    @HystrixCommand(fallbackMethod = "paymentinfo_Timeout_Handler", commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "4000")
    })
    public String paymentinfo_Timeout(Integer id){
        int interTime = 3;
        // int age = 10/0;  // 演示报错
        try{
            TimeUnit.SECONDS.sleep(interTime);  // 模拟超时
        }catch (Exception e){
            e.printStackTrace();
        }
        log.info("hystrix payment service!!!");
        return "线程池：" + Thread.currentThread().getName() + "--paymentInfo_Timeout，id:" + id +
                "耗时" + interTime + "秒钟--";
    }

    /**
     * paymentinfo_Timeout 方法失败后 自动调用此方法 实现服务降级 告知调用者 paymentinfo_Timeout 目前无法正常调用
     * @param id
     * @return
     */
    public String paymentinfo_Timeout_Handler(Integer id){
        return "线程池:  " + Thread.currentThread().getName() + "  paymentInfoTimeOutHandler8001系统繁忙或者运行报错，请稍后再试,id:  " + id + "\t"
                + "o(╥﹏╥)o";
    }



    /**
     * 服务熔断 超时、异常、都会触发熔断
     * 1、默认是最近10秒内收到不小于10个请求
     * 2、并且有60%是失败的
     * 3、就开启断路器
     * 4、开启后所有请求不再转发，降级逻辑自动切换为主逻辑，减小调用方的响应时间
     * 5、经过一段时间（时间窗口期，默认是5秒），断路器变为半开状态，会让其中一个请求进行转发
     *      5.1、如果成功，断路器会关闭
     *      5.2、若失败，继续开启。重复4和5
     *
     * @param id
     * @return
     */
    @HystrixCommand(fallbackMethod = "paymentCircuitBreakerFallback", commandProperties = {
            @HystrixProperty(name = "circuitBreaker.enabled", value = "true"),  // 是否开启断路器
            @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "10"),  // 请求次数
            @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "10000"),  // 时间窗口期
            @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "60"),  // 失败率达到多少后跳闸
    })
    public String paymentCircuitBreaker(Integer id) {
        if (id < 0) {
            throw new RuntimeException("******id 不能负数");
        }

        String serialNumber = IdUtil.simpleUUID();
        return Thread.currentThread().getName() + "\t" + "调用成功，流水号: " + serialNumber;
    }


    /**
     * paymentCircuitBreaker 方法的 fallback
     * 当断路器开启时，主逻辑熔断降级，该 fallback 方法就会替换原 paymentCircuitBreaker 方法，处理请求
     *
     * @param id
     * @return
     */
    public String paymentCircuitBreakerFallback(Integer id) {
        return "fallback----------" + Thread.currentThread().getName() + "\t" + "id 不能负数或超时或自身错误，请稍后再试  id: " + id;
    }
}
