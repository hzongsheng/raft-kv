package raft.core.schedule;

import java.util.Random;
import java.util.concurrent.*;

/**
 * 包括可设置的最大/小选举超时时间, 日志复制间隔
 *
 * 可以提交一个日志任务, 返回一个 scheduledFuture 的包装类 LogReplicationTask
 *
 * 提交一个选举超时任务, 返回一个 scheduledFuture 的包装类 ElectionTimeout
 *
 */
public class DefaultScheduler implements Scheduler{
    /**
     * 最小选举超时时间
     * 最大选举超时时间
     * *初次* 日志复制延迟时间
     * 日志复制时间间隔
     * 发起选举随机延时
     * 用于运行日志复制和超时任务的线程池
     */
    private final int minElectionTimeout;
    private final int maxElectionTimeout;
    private final int logReplicationDelay;
    private final int logReplicationInterval;
    private final Random electionTimeoutRandom;
    private final ScheduledExecutorService scheduledExecutorService;

    /**
     * 创建默认调度器
     *
     * @param minElectionTimeout 最小选举时间,毫秒
     * @param maxElectionTimeout 最大选举时间, 毫秒
     * @param logReplicationDelay 初次日志复制延迟, 毫秒
     * @param logReplicationInterval 日志间隔, 毫秒
     */
    public DefaultScheduler(int minElectionTimeout, int maxElectionTimeout, int logReplicationDelay, int logReplicationInterval) {
        if(minElectionTimeout <= 0 || maxElectionTimeout <= 0 || minElectionTimeout > maxElectionTimeout){
            throw new IllegalArgumentException(
                    "election timeout should not be 0 or min > max"
            );
        }

        if(logReplicationDelay < 0 || logReplicationInterval <= 0){
            throw new IllegalArgumentException(
                    "log replication delay < 0 or log replication interval <= 0"
            );
        }

        this.minElectionTimeout = minElectionTimeout;
        this.maxElectionTimeout = maxElectionTimeout;
        this.logReplicationDelay = logReplicationDelay;
        this.logReplicationInterval = logReplicationInterval;
        electionTimeoutRandom = new Random();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "scheduler"));
    }

    @Override
    public LogReplicationTask scheduleLogReplicationTask(Runnable task) {
        ScheduledFuture<?> scheduledFuture = this.scheduledExecutorService.scheduleWithFixedDelay(
                task, logReplicationDelay, logReplicationInterval, TimeUnit.MILLISECONDS);
        return new LogReplicationTask(scheduledFuture);
    }

    /**
     * 提交一个选举超时任务, 将在最小超时和最大超时间隔内超时
     *
     * @param task task
     * @return ElectionTimeout scheduledFuture 的包装类
     */
    @Override
    public ElectionTimeout scheduleElectionTimeout(Runnable task) {
        int timeout = electionTimeoutRandom.nextInt(maxElectionTimeout - minElectionTimeout ) + minElectionTimeout;
        ScheduledFuture<?> scheduledFuture = scheduledExecutorService.schedule(task, timeout, TimeUnit.MILLISECONDS);
        return  new ElectionTimeout(scheduledFuture);
    }

    @Override
    public void stop() throws InterruptedException {
        scheduledExecutorService.shutdown();
        scheduledExecutorService.awaitTermination(1, TimeUnit.SECONDS);
    }
}
