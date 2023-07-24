package com.zyl.springcloud.controller;

import com.zyl.springcloud.service.IMessageProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class SendMessageController {

    @Resource
    IMessageProvider messageProvider;

    @GetMapping("/sendMessage")
    public String sendMessage() {
        String messageContent = messageProvider.send();  // 自己定义的方法，但是里面调用了MessageChannel.send()方法
        return "messageContent:" + messageContent;
    }
}
