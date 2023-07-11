package com.zyl.springcloud.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;  //网络客户端

@Configuration
public class ApplicationContextConfig {

    @Bean
    @LoadBalanced  // 注解的作用：赋予了RestTemplate 负载均衡的能力
    public RestTemplate getRestTemplate(){
        return new RestTemplate();
        /*
        RestTemplate提供了多种便捷访问远程http服务的方法，
        是一种简单便捷的访问restful服务模板类，是spring提供的用于rest服务的客户端模板工具集
        */
    }
}
