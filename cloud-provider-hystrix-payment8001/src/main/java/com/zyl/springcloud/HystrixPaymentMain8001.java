package com.zyl.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
//注解开启断路器功能
@EnableCircuitBreaker
public class HystrixPaymentMain8001 {
    public static void main(String[] args) {
        SpringApplication.run(HystrixPaymentMain8001.class, args);
    }
}
