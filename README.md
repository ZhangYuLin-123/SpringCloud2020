[toc]

# SpringCloud2020
## cloud-api-common
模块概述：将后面许多模块都要用到的类统一抽取放到该模块中，install后供其他模块引用。

## cloud-consumer-order80
模块概述：订单服务，注册在eureka注册中心。配置restTemplate使用@LoadBalanced注解，赋予了restTemplate负载均衡能力，使用restTemplate来调用注册在eureka上的支付服务即**cloud-provider-payment8001**、**cloud-provider-payment8002**。

### restTemplate
对于RestTemplate 的一些说明：

有两种请求方式：post和get ,还有两种返回类型：object 和 Entity

* getForObject()/getForEntity()
  * Object：返回对象响应体中数据转化成的对象，基本上可以理解成json
  * Entity：返回对象是ResponseEntity对象，包含了响应中的一些重要信息，比如响应头、响应状态码、响应体等
返回的entity.getBody()即得到了Object
* postForObject()/postForEntity()
### ribbon负载均衡
IRule：根据特定算法从服务列表中选择一个要访问的服务

Ribbon 负载均衡规则类型：
* com.netflix.loadbalancer.RoundRobinRule：轮询
* com.netflix.loadbalancer.RandomRule：随机
* com.netfIix.IoadbaIancer.RetryRuIe：先按照RoundRobinRule的策略获取服务，如果获取服务失败则在指定时间内会进行重试，获取可用的服务
* WeightedResponseTimeRule：对RoundRobinRule的扩展，响应速度越快的实例选择权重越大，越容易被选择
* BestAvailableRule：会先过滤掉由于多次访问故障而处于断路器跳闸状态的服务，然后选择一个并发量最小的服务
* AvailabilityFilteringRule：先过滤掉故障实例，再选择并发较小的实例
* ZoneAvoidanceRule：默认规则，复合判断server所在区域的性能和server的可用性选择服务器

#### 针对某个provider服务，不使用默认的轮询策略，选用随机策略
com.zyl.myrule.MyRibbonRule + 主启动类加注解@RibbonClient并指定针对哪个服务
注意点：MyRibbonRule不要放在SpringBoot主启动类的包内，不然除了指定的那个服务，调用其他微服务，一律使用这一种负载均衡算法
```java
@Configuration
public class MyRibbonRule {
    @Bean
    public IRule myRandomRule() {
        return new RandomRule();  // 指定使用  随机  的规则
    }
}
```
```java
@SpringBootApplication
@EnableEurekaClient
@RibbonClient(name = "CLOUD-PAYMENT-SERVICE", configuration = MyRibbonRule.class)  // 指定该负载均衡规则对哪个提供者服务使用，加载自定义规则的配置类
public class OrderMain80{
    public static void main(String[] args){
        SpringApplication.run(OrderMain80.class,args);
    }
}
```
#### 修改全局负载均衡策略为随机
com.zyl.springcloud.config.MyRibbonRuleConfig
```java
@Configuration
public class MyRibbonRuleConfig {
    @Bean
    public IRule myRule() {
        return new RandomRule();
    }
}
```
#### 自己模拟实现轮询策略
com.zyl.springcloud.lb.MyLoadBalancer
```java
public interface MyLoadBalancer {
    ServiceInstance instances(List<ServiceInstance> serviceInstances);
}
```
com.zyl.springcloud.lb.MyLoadBalancerImpl
实现具体的轮询策略  用到的技术点：自旋 + CAS
```java
@Component
public class MyLoadBalancerImpl implements MyLoadBalancer{

    private AtomicInteger atomicInteger = new AtomicInteger(0);

    private final int getAndIncrement() {
        int current;
        int next;

        // 自旋 + CAS
        do {
            current = this.atomicInteger.get();
            next = current >= Integer.MAX_VALUE ? 0 : current + 1;
        } while (!atomicInteger.compareAndSet(current, next));
        System.out.println("第几次访问,次数next:" + next);
        return next;
    }

    @Override
    public ServiceInstance instances(List<ServiceInstance> serviceInstances) {
        int index = getAndIncrement() % serviceInstances.size();
        return serviceInstances.get(index);
    }
}
```
com.zyl.springcloud.controller.OrderController
测试自己的轮询策略
```java
@RestController
@Slf4j
@RequestMapping("/consumer")
public class OrderController {
    
    public static final String PAYMENT_URL = "http://CLOUD-PAYMENT-SERVICE";

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private MyLoadBalancer myLoadBalancer;

    @Resource
    private DiscoveryClient discoveryClient;
    
    

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
```


## cloud-eureka-server7001
模块概述：eurek服务注册中心，与**cloud-eureka-server7002**相互注册，互相守望，形成集群。

## cloud-eureka-server7002
模块概述：类似**cloud-euraka-server7001**。

## cloud-provider-payment8001
模块概述：支付服务，注册在eureka注册中心，和**cloud-provider-payment8002**形成集群，供**cloud-consumer-order80**对应的服务调用。

## cloud-provider-payment8002
模块概述：类似**cloud-provider-payment8001**。

## cloud-consumer-zk-order80
模块概述：订单服务，注册到zookeeper。

注意包版本的问题:
如果 zookeeper 的版本和导入的jar包版本不一致，启动就会报错，由zk-discovery和zk之间的jar包冲突的问题
解决这种冲突:需要在 pom 文件中，排除掉引起冲突的jar包，添加和服务器zookeeper版本一致的 jar 包
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-zookeeper-discovery</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.apache.zookeeper</groupId>
            <artifactId>zookeeper</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.apache.zookeeper</groupId>
    <artifactId>zookeeper</artifactId>
    <version>3.8.0</version>
</dependency>
```

## cloud-provider-zk-payment8004
模块概述：支付服务，注册到zookeeper。


## cloud-consumer-consul-order80
模块概述：订单服务，注册到consul

### consul
* consul也是服务注册中心的一个实现，是由go语言写的
* 官网地址： https://www.consul.io/intro 
* 中文地址： https://www.springcloud.cc/spring-cloud-consul.html
* Consul是一套开源的分布式服务发现和配置管理系统，提供了微服务系统中的服务治理，配置中心，控制总线等功能。这些功能中的每一个都可以根据需要单独使用，也可以一起使用以构建全方位的服务网络。

## cloud-provider-consul-payment8006
模块概述：支付服务，注册到consul

## cloud-consumer-feign-order80
模块概述：订单服务，服务注册在eureka，利用feign来调用**cloud-provider-payment8001**和**cloud-provider-payment8002**中的服务，feign依赖中自带了ribbon

### Feign

Feign旨在使编写JavaHttp客户端变得更容易。

前面在使用Ribbon+RestTemplate时，利用RestTemplate对http请求的封装处理，形成了一套模版化的调用方法。但是在实际开发中，由于对服务依赖的调用可能不止一处，往往一个接囗会被多处调用，所以通常都会针对每个微服务自行封装些客户端类来包装这些依赖服务的调用。所以，Feign在此基础上做了进一步封装，由他来帮助我们定义和实现依赖服务接口的定义。在Feign的实现下，我们只需创建一个接口并使用注解的方式来配置它（以前是Dao接口上面标注Mapper注解，现在是一个微服务接口上面标注一个Feign注解即可，即可完成对服务提供方的接口绑定，简化了使用SpringCloud Ribbon时，自动封装服务调用客户端的开发量。

Feign集成了Ribbon

利用Ribbon维护了Payment的服务列表信息，并目通过轮询实现了客户端的负载均衡。而与Ribbon不同的是，通过feign只需要定义服务绑定接口目以声明式的方法，优雅而简单的实现了服务调用

|  Feign   | OpenFeign  |
|  ----  | ----  |
|  Feign是SpringCloud组件中的一个轻量级RESTful的HTTP服务客户端。Feign内置了Ribbon，用来做客户端负载均衡，去调用服务注册中心的服务。Feign的使用方式是：使用Feign的注解定义接口，调用这个接口，就可以调用服务注册中心的服务  | OpenFeign是SpringCloud在Feign的基础上支持了SpringMVC的注解，如@RequestMapping等等。OpenFeign的@FeignClient可以解析SpringMVC的下的接囗，并通过动态代理的方式产生实现类，实现类中做负载均衡并调用其他服务。  |
| org.springframework.cloud spring-cloud-starter-feign  | org.springframework.cloud spring-cloud-starter-openfeign |
	

#### Feign 超时控制
application.yml
```yml
#设置feign客户端超时时间(OpenFeign默认支持ribbon)
ribbon:
  #指的是建立连接所用的时间，适用于网络状况正常的情况下,两端连接所用的时间
  ReadTimeout: 5000
  #指的是建立连接后从服务器读取到可用资源所用的时间
  ConnectTimeout: 5000
```
	
#### Feign 日志打印
```java
package com.zyl.springcloud.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    /**
     * 日志级别：
     *
     * NONE：默认的，不显示任何日志
     * BASIC：仅记录请求方法、URL、响应状态码及执行时间
     * HEADERS：除了BASIC中定义的信息之外，还有请求和响应的头信息
     * FULL：除了HEADERS中定义的信息之外，还有请求和响应的正文及元数据
     * @return
     */
    @Bean
    Logger.Level feignLoggerLevel(){
        return Logger.Level.FULL;
    }
}
```