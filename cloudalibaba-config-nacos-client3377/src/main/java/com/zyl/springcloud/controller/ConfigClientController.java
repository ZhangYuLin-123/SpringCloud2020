package com.zyl.springcloud.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope  //支持Nacos的动态刷新
public class ConfigClientController {

    @Value("${config.info}") // 从nacos中取
    private String configInfo;

    @GetMapping("/configclient/getconfiginfo")
    public String getConfigInfo(){
        return configInfo;
    }
}
