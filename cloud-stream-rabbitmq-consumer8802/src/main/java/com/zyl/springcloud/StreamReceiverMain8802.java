package com.zyl.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class StreamReceiverMain8802 {
    public static void main(String[] args) {
        SpringApplication.run(StreamReceiverMain8802.class, args);
    }
}