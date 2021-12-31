package raft.core.node.store;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import raft.core.support.TaskExecutor;

import java.util.Collections;

public abstract class AbstractTaskExecutor implements TaskExecutor {

    @Override
    public void submit(Runnable task, FutureCallback<Object> callback) {
        Preconditions.checkNotNull(task);
        Preconditions.checkNotNull(callback);
        submit(task, Collections.singletonList(callback));
    }
}
