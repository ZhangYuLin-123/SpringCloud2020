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

## cloud-provider-zk-payment8004
模块概述：支付服务，注册到zookeeper。

### zookeeper
zookeeper是一个分布式协调工具，可以实现注册中心功能

## cloud-consumer-consul-order80
模块概述：订单服务，注册到consul

### consul
* consul也是服务注册中心的一个实现，是由go语言写的
* 官网地址： https://www.consul.io/intro 
* 中文地址： https://www.springcloud.cc/spring-cloud-consul.html
* Consul是一套开源的分布式服务发现和配置管理系统，提供了微服务系统中的服务治理，配置中心，控制总线等功能。这些功能中的每一个都可以根据需要单独使用，也可以一起使用以构建全方位的服务网络。

## cloud-provider-consul-payment8006
模块概述：支付服务，注册到consul