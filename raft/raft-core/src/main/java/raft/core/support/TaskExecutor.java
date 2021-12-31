package raft.core.support;

import com.google.common.util.concurrent.FutureCallback;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface TaskExecutor {
    /**
     * 提交一个无返回结果的任务
     *
     * @param task 无返回结果的任务
     * @return Future 异步任务的 Future
     */
    Future<?> submit(Runnable task);

    /**
     * 提交有返回结果的任务
     *
     * @param task 提交的任务
     * @return Future 返回一个 Future 对象
     */
    <V> Future<V> submit(Callable<V> task);

    void submit( Runnable task,  FutureCallback<Object> callback);

    void submit(Runnable task, Collection<FutureCallback<Object>> callbacks);

    void shutdown() throws InterruptedException;
}