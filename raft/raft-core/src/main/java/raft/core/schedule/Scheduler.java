package raft.core.schedule;

public interface Scheduler {
    /**
     * 配置日志超时任务
     *
     * @param task task
     * @return LogReplicationTask
     */
    LogReplicationTask scheduleLogReplicationTask(Runnable task);

    /**
     * 配置选举超时任务
     *
     * @param task task
     * @return ElectionTimeout
     */
    ElectionTimeout scheduleElectionTimeout(Runnable task);

    /**
     * 关闭调度器
     *
     * @throws InterruptedException 如果被中断
     */
    void stop() throws InterruptedException;
}


