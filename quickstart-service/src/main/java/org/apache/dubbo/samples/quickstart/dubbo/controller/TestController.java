package org.apache.dubbo.samples.quickstart.dubbo.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.samples.quickstart.dubbo.api.DemoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author chenyunbin
 * @date 2026/7/29 下午10:26
 */
@RestController
@RequestMapping("/dubbo/test")
public class TestController {

    //version来实现版本灰度发布，实现多版本控制
    //loadbalance来配置负载均衡策略: org.apache.dubbo.rpc.cluster.LoadBalance 对应的源码类
    // random：按权重随机
    // roundrobin：按权重轮训， 1 2 1 三个提供者， 2应该会执行2次之后在重新轮询。
    // leastactive：最少活跃调用数，相同活跃数的随机
    // consistenthash：一致性hash，相同参数的请求总是会发送同同一个【提供者】。


    //集群容错机制 org.apache.dubbo.rpc.cluster.Cluster 对应的源码类，默认机制是failover
    // failover：出现失败会重试其他机器。retries = 3设置重试次数。
    // failfast：快速失败，请求失败快速返回异常，不做重试。
    // failsafe：出现异常直接忽略。不关心调用是否成功，并且不想因为异常影响外层调用(一般用在无关紧要的服务，如日志。)
    // failback：请求失败，记录在失败队列中。使用异步线程池来重试执行。
    // forking：同时调用多个服务，只要其中一个返回，则立即返回结果。
    //    - 可配置forks = "最大并行调用数"。
    //    - 一般用在实时性较高的服务
    // broadcast：广播调用所有可用服务，一个报错就报错。 不需要负载均衡
    //    - 通常是会用在服务状态更新后的广播。
    // available：最简单的方式，遍历所有服务列表，找到一个可用的节点调用直接返回。会有节点返回异常。
    // zone-aware: 多注册中心场景‌下的集群负载均衡策略，旨在实现基于地域（Zone）或优先级的流量调度。
    // 该策略自 ‌Dubbo 2.7.5‌ 版本引入，默认在多注册中心订阅时生效，通过 ZoneAwareCluster 实现。
    // mergeable：主要用于‌聚合多个服务分组（Group）的调用结果‌。当同一个接口存在多种实现（通过 group 区分），
    // 且消费方需要同时调用所有实现并合并返回数据时，该模式非常适用（例如聚合不同来源的菜单项）。

    //    @DubboReference(scope = "remote", timeout = 3000, version = "2.0",loadbalance = "random",cluster = "failover",retries = 3)
    @DubboReference(version = "2.0")
    private DemoService demoService;

    int i = 1;

    //,
//            fallback = "handleFallback"
    @SentinelResource(value = "sayHello",
            blockHandler = "handleBlock",
            fallback = "handleFallback")  // 增加 fallback 处理业务异常
    @GetMapping("/sayHello")
    public String sayHello(@RequestParam("name") String name) {

        if ("error".equals(name)) {
            throw new RuntimeException("模拟业务异常报错！");
        }

        return demoService.sayHello(name);
    }

    // blockHandler 方法必须包含同样的参数，最后加 BlockException
    // 1. blockHandler：处理限流/降级，参数最后多一个 BlockException
    public String handleBlock(String name, BlockException ex) {
        // 处理被限流或被降级的情况
        return name + "  请求太频繁，请稍后再试！";
    }

    // 新增 fallback 方法
    public String handleFallback(String name, Throwable throwable) {
        return "业务逻辑报错，触发降级: " + throwable.getMessage();
    }


//    原文链接：https://blog.csdn.net/m0_48038376/article/details/141421983


//     new  Thread(()->{
//
//        while (true){
//            System.out.println("sayHello==="+i++);
//
//
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }).start();

}
