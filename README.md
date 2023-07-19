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
application.yml配置：
```yml
logging:
  level:
    # feign日志以什么级别监控哪个接口
    com.zyl.springcloud.service.OrderFeignService: debug
```

## cloud-consumer-feign-hystrix-order80
模块概述：服务注册到eureka，使用feign调用**cloud-provider-hystrix-payment8001**中的服务，@FeignClient中配置fallback进行服务降级

### 服务降级
服务降级，客户端去调用服务端，碰上服务端宕机或关闭。
本次案例降级处理是在客户端80实现完成的，与服务端8001没有关系。只需要为Feign客户端定义的接口添加一个服务降级处理的实现类即可实现解耦。

#### feign + fallback
重要依赖：
```
<!--openfeign  自带feign-hystrix  自带ribbon-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```
PaymentHystrixService:通过feign调用服务提供者的方法，配置fallback进行降级处理
```
@Component
// FeignFallback 客户端的服务降级 针对 CLOUD-PROVIDER-HYSTRIX-PAYMENT 该服务 提供了一个 对应的服务降级类
@FeignClient(value = "CLOUD-PROVIDER-HYSTRIX-PAYMENT", fallback = PaymentFallbackServiceImpl.class)
// @FeignClient(value = "CLOUD-PROVIDER-HYSTRIX-PAYMENT")
public interface PaymentHystrixService {
    @GetMapping("/payment/hystrix/{id}")
    String paymentInfoOK(@PathVariable("id") Integer id);

    @GetMapping("/payment/hystrix/timeout/{id}")
    String paymentInfoTimeOut(@PathVariable("id") Integer id);
}
```
PaymentFallbackServiceImpl：PaymentHystrixService接口实现类，里面是服务降级后的兜底方案
```
@Component
public class PaymentFallbackServiceImpl implements PaymentHystrixService {
    @Override
    public String paymentInfoOK(Integer id) {
        return "PaymentFallbackService fall back-paymentInfo_OK ,o(╥﹏╥)o";
    }

    @Override
    public String paymentInfoTimeOut(Integer id) {
        return "PaymentFallbackService fall back-paymentInfo_TimeOut ,o(╥﹏╥)o";
    }

}
```
application.yml:
```
server:
  port: 80

spring:
  application:
    name: cloud-order-service  # 项目名,也是注册的名字

  cloud:
    inetutils:
      ignored-interfaces: [ 'VMware.*' ]  # 注册到Eureka上的服务，注册的IP是虚拟机的IP的解决方案

eureka:
  client:
    # 注册进 Eureka 的服务中心
    register-with-eureka: true
    # 检索 服务中心 的其它服务
    fetch-registry: true
    service-url:
      # 设置与 Eureka Server 交互的地址
      # defaultZone: http://localhost:7001/eureka/ # 注册中心为单机时
      defaultZone: http://eureka7001.com:7001/eureka/,http://eureka7002.com:7002/eureka/  # 注册中心为集群时
  instance: #重点，和client平行
    instance-id: order80 # 每个提供者的id不同，显示的不再是默认的项目名
    prefer-ip-address: true # 可以显示ip地址
    # Eureka客户端向服务端发送心跳的时间间隔，单位s，默认30s
    lease-renewal-interval-in-seconds: 1
    # Rureka服务端在收到最后一次心跳后等待时间上线，单位为s，默认90s，超时将剔除服务
    lease-expiration-duration-in-seconds: 2


#设置feign客户端超时时间(OpenFeign默认支持ribbon)
ribbon:
  #指的是建立连接所用的时间，适用于网络状况正常的情况下,两端连接所用的时间
  ReadTimeout: 5000
  #指的是建立连接后从服务器读取到可用资源所用的时间
  ConnectTimeout: 5000

logging:
  level:
    # feign日志以什么级别监控哪个接口
    com.zyl.springcloud.service.OrderFeignService: debug


# 用于服务降级 在注解@FeignClient 中添加 fallback 属性值
feign:
  hystrix:
    enabled: true

# 注解@FeignClient 中添加 fallback 属性值，但默认超时时间为1秒（如果调用生产者的服务超过1秒则直接fallback）
hystrix:
  command:
    default:
      execution:
        timeout:
          enable: true  # 默认值   为false则超时控制有ribbon控制，为true则hystrix超时和ribbon超时都是用，但是谁小谁生效
        isolation:
          thread:
            timeoutInMilliseconds: 5000
```


## cloud-provider-hystrix-payment8001
模块概述：服务注册到eureka，提供方法供**cloud-consumer-feign-hystrix-order80**调用，改模块中部分方法进行了熔断处理

### 降级
@HystrixCommand降级可以放在服务提供端，也可以放在消费端，但一般放在消费端
```
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
}
```
### 熔断
服务雪崩：
多个微服务之间调用的时候，假设微服务A调用微服务B和微服务C，微服务B和微服务C有调用其他的微服务，这就是所谓的”扇出”，如扇出的链路上某个微服务的调用响应式过长或者不可用，对微服务A的调用就会占用越来越多的系统资源，进而引起系统雪崩，所谓的”雪崩效应”

Hystrix：
Hystrix是一个用于分布式系统的延迟和容错的开源库。在分布式系统里，许多依赖不可避免的调用失败，比如超时、异常等，Hystrix能够保证在一个依赖出问题的情况下，不会导致整个服务失败，避免级联故障，以提高分布式系统的弹性。

断路器：
“断路器”本身是一种开关装置，当某个服务单元发生故障监控(类似熔断保险丝)，向调用方法返回一个符合预期的、可处理的备选响应(FallBack)，而不是长时间的等待或者抛出调用方法无法处理的异常，这样就保证了服务调用方的线程不会被长时间、不必要地占用，从而避免了故障在分布式系统中的蔓延。乃至雪崩。

服务熔断：
熔断机制是应对雪崩效应的一种微服务链路保护机制，当扇出链路的某个微服务不可用或者响应时间太长时，会进行服务的降级，进而熔断该节点微服务的调用，快速返回”错误”的响应信息。
当检测到该节点微服务响应正常后恢复调用链路，在SpringCloud框架机制通过Hystrix实现，Hystrix会监控微服务见调用的状况，当失败的调用到一个阈值，默认是5秒内20次调用失败就会启动熔断机制，熔断机制的注解是@HystrixCommand


熔断的状态：
* 熔断打开：请求不再进行调用当前服务，内部设置时钟一般为MTTR（平均故障处理时间），当打开时长达到所设时钟则进入半熔断状态
* 熔断关闭：熔断关闭不会对服务进行熔断
* 熔断半开：部分请求根据规则调用当前服务，如果请求成功目符合规则，则认为当前服务恢复正常，关闭熔断

#### 熔断案例
PaymentService:
```
@Service
@Slf4j
public class PaymentService {
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
```
涉及到断路器的三个重要参数快照时间窗、请求总数阀值、错误百分比阀值
1：快照时间窗：断路器确定是否打开需要统计一些请求和错误数据而统计的时间范围就是快照时间窗，默认为最近的10秒。
2：请求总数阀值：在快照时间窗内，必须满足请求总数阀值才有资格熔断。默认为20，意味着在10秒内，如果该hystrix命令的调用次数不足20次，即使所有的请求都超时或具他原因失败，断路器都不会打开。
3：错误百分比阀值：当请求总数在快照时间窗内超过了阀值，上日发生了30次调用，如果在这30次调用中，有15次发生了超时异常，也就是超过50％的错误百分比，在默认设定50％阀值情况，这时候就会将断路器打开。


## cloud-hystrix-dashboard9001
模块概述：利用Hystrix调用监控(HystrixDashboard)，监控**cloud-provider-hystrix-payment8001**中的服务被调用的情况

对模块cloud-provider-hystrix-payment8001的调整：
```java
@SpringBootApplication
@EnableEurekaClient
//注解开启断路器功能
@EnableCircuitBreaker
public class HystrixPaymentMain8001 {
    public static void main(String[] args) {
        SpringApplication.run(HystrixPaymentMain8001.class, args);
    }

    /**
     * 注意：新版本Hystrix需要在主启动类中指定监控路径
     * 此配置是为了服务监控而配置，与服务容错本身无关，spring cloud升级后的坑
     * ServletRegistrationBean因为springboot的默认路径不是"/hystrix.stream"，
     * 只要在自己的项目里配置上下面的servlet就可以了
     *
     * @return ServletRegistrationBean
     */
    @Bean
    public ServletRegistrationBean getServlet() {
        HystrixMetricsStreamServlet streamServlet = new HystrixMetricsStreamServlet();
        ServletRegistrationBean registrationBean = new ServletRegistrationBean(streamServlet);

        // 一启动就加载
        registrationBean.setLoadOnStartup(1);
        // 添加url
        registrationBean.addUrlMappings("/hystrix.stream");
        // 设置名称
        registrationBean.setName("HystrixMetricsStreamServlet");
        return registrationBean;
    }
}
```

启动测试：访问 http://ocalhost:9001/hystrix，
对8001进行监控：输入 http://localhost:8001/hystrix.stream