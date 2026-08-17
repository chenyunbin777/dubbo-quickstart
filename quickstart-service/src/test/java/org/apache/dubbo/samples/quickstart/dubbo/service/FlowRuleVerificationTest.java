package org.apache.dubbo.samples.quickstart.dubbo.service;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * QPS 阈值成功加载为 100
 * 连续突发 130 次调用时产生 FlowException，证明限流生效
 * 连续记录 2 次业务异常后产生 DegradeException，证明异常比例熔断生效
 * 熔断比例为 50%
 * 熔断时长为 10 秒
 */
class FlowRuleVerificationTest {

    @BeforeEach
    void loadConfiguredRules() {
        FlowRuleManager.loadRules(null);
        DegradeRuleManager.loadRules(null);
        new FlowRuleInit().init();
    }

    @Test
    void configuredRulesAreLoaded() {
        assertEquals(100, FlowRuleManager.getRules().get(0).getCount());
        assertEquals(2, DegradeRuleManager.getRules().get(0).getMinRequestAmount());
        assertEquals(0.5, DegradeRuleManager.getRules().get(0).getCount());
        assertEquals(10, DegradeRuleManager.getRules().get(0).getTimeWindow());
    }

    /**
     * 限流测试
     * @throws Exception
     */
    @Test
    void qpsRuleRejectsRequestsAboveThreshold() throws Exception {
        int blocked = 0;
        for (int i = 0; i < 130; i++) {
            // 向 Sentinel 申请进入 sayHello 资源
            try (Entry ignored = SphU.entry("sayHello")) {
                // Pass.
            } catch (FlowException ex) {
                // 抛出 FlowException，表示本次请求被限流
                blocked++;
            }
        }
        assertTrue(blocked > 0, "QPS=100 should reject part of a 130-request burst");
    }


    /**
     * 熔断测试
     * @throws Exception
     */
    @Test
    void exceptionRatioRuleOpensCircuit() throws Exception {
        // Let any QPS metrics produced by another test leave the one-second window.
        Thread.sleep(1100);
        recordBusinessFailure();
        recordBusinessFailure();

        BlockException blocked = assertThrows(BlockException.class,
                () -> SphU.entry("sayHello"));
        assertTrue(blocked instanceof DegradeException,
                "business failures should open the degrade circuit");
    }

    private void recordBusinessFailure() throws BlockException {
        Entry entry = SphU.entry("sayHello");
        try {
            Tracer.traceEntry(new RuntimeException("test failure"), entry);
        } finally {
            entry.exit();
        }
    }
}
