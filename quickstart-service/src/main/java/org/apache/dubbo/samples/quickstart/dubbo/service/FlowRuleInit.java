package org.apache.dubbo.samples.quickstart.dubbo.service;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 设置限流规则、熔断规则
 *
 * 参考官方文档：https://sentinelguard.io/zh-cn/docs/circuit-breaking.html
 *
 * @author cyb
 * @date 2026/8/4 23:09
 */
@Component
public class FlowRuleInit {


    @PostConstruct
    public void init() {
        initFlowRules();
        initDegradeRules();
//        initAuthorityRules();

    }

    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();

        // 设置资源名称，需与 @SentinelResource 的 value 一致
        rule.setResource("sayHello");
        // 限流阈值类型：QPS 模式
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        // 每秒最多允许 10 个请求
        rule.setCount(100);
        // 流控效果：直接拒绝
        rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);

        rules.add(rule);
        // 加载规则
        FlowRuleManager.loadRules(rules);

        System.out.println(">>> Sentinel 测试规则已加载: testResource QPS=100");


    }


    /**
     * 熔断降级规则
     * 注意异常降级仅针对业务异常，对 Sentinel 限流降级本身的异常（BlockException）不生效。为了统计异常比例或异常数，需要通过 Tracer.trace(ex) 记录业务异常。示例：
     */
    private void initDegradeRules() {

        DegradeRule rule = new DegradeRule("sayHello");

        // -----熔断策略：慢调用比例----- 选择以慢调用比例作为阈值，需要设置允许的慢调用 RT（即最大的响应时间），请求的响应时间大于该值则统计为慢调用
//        rule.setGrade(RuleConstant.DEGRADE_DEFAULT_SLOW_REQUEST_AMOUNT);

        //单位统计时长内（statIntervalMs）：请求数目大于设置的最小请求数目（minRequestAmount），并且慢调用的比例大于阈值（count），则接下来的熔断时长内请求会自动被熔断。


        // -----熔断策略：慢调用比例-----


        // 熔断策略：异常比例
        rule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);

        //在有效统计时间范围内，可触发断路机制的最小请求数。 默认值是5
        //这个要根据具体的负载数量来动态调整
        rule.setMinRequestAmount(2);

        //单位统计时长:ms，默认值：1000ms
        rule.setStatIntervalMs(1000);
        // 比例阈值为 50%
        // 1、在平均响应时间（RT）模式下，它表示以毫秒为单位的最大响应时间（RT）
        // 2、在异常比率模式下，它表示介于0.0和1.0之间的异常比率。
        // 3、在异常计数模式下，它表示异常计数
        rule.setCount(0.5);
        // 熔断时长 10 秒
        rule.setTimeWindow(10);



        DegradeRuleManager.loadRules(Collections.singletonList(rule));


        System.out.println(">>> Sentinel 测试规则已加载: initDegradeRules 熔断时长 10 秒");

    }
//
//
//    //    系统保护规则（SystemRule）：使用 SystemRuleManager.loadRules()。
////
////    访问控制规则（AuthorityRule）：使用 AuthorityRuleManager.loadRules()
//    private void initAuthorityRules() {
//
//        AuthorityRule rule = new AuthorityRule();
//        rule.setResource("sayHello");                // 资源名称，与 @SentinelResource 的 value 一致
//        rule.setStrategy(RuleConstant.AUTHORITY_WHITE); // 白名单模式（亦可设为黑名单）
//        rule.setLimitApp("trusted-app");             // 只允许 origin = "trusted-app" 的请求通过
//
//        // 加载规则
//        AuthorityRuleManager.loadRules(Collections.singletonList(rule));
//        System.out.println(">>> AuthorityRule 加载完成：只允许 origin=trusted-app 访问资源 sayHello");
//
//    }


}
