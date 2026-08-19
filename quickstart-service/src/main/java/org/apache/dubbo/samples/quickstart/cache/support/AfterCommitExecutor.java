package org.apache.dubbo.samples.quickstart.cache.support;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务提交后动作执行器。
 * <p>
 * 缓存更新必须晚于 DB 提交，否则 DB 回滚后 Redis 可能保留一份并不存在的新数据。
 * 如果当前存在真实事务，本类把动作注册到 afterCommit；如果调用方没有开启事务，则立即执行。
 */
@Component
public class AfterCommitExecutor {

    /**
     * 在当前事务成功提交后执行动作。事务回滚不会触发动作；无事务时同步执行。
     *
     * @param action 通常是更新或删除 Redis 的操作
     */
    public void execute(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
