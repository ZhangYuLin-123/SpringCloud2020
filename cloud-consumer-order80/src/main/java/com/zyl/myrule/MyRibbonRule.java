package com.zyl.myrule;

import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.RandomRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyRibbonRule {
    @Bean
    public IRule myRandomRule() {
        return new RandomRule();  // 指定使用  随机  的规则
    }
}
