package raft.core.log.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.core.support.SingleThreadTaskExecutor;
import raft.core.support.TaskExecutor;

import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractSingleThreadStateMachine implements StateMachine{

    private static final Logger logger = LoggerFactory.getLogger(AbstractSingleThreadStateMachine.class);
    /** 处理线程和核心组件线程都会访问 lasApplied , 但只有处理线程修改, 这种一写多读场景使用 volatile */
    private volatile int lastApplied = 0;
    private final TaskExecutor taskExecutor;

    public AbstractSingleThreadStateMachine() {
        taskExecutor = new SingleThreadTaskExecutor("state-machine");
    }

    @Override
    public int getLastApplied() {
        return lastApplied;
    }

    @Override
    public void applyLog(StateMachineContext context, int index,  byte[] commandBytes, int firstLogIndex) {
        // 单线程处理 log
        taskExecutor.submit(() -> doApplyLog(context, index, commandBytes, firstLogIndex));
    }

    private void doApplyLog(StateMachineContext context, int index,  byte[] commandBytes, int firstLogIndex) {
        if (index <= lastApplied) {
            return;
        }
        logger.debug("apply log {}", index);
        applyCommand(commandBytes);
        lastApplied = index;
    }

    protected abstract void applyCommand( byte[] commandBytes);

    @Override
    public void shutdown() {
        try {
            taskExecutor.shutdown();
        } catch (InterruptedException e) {
            throw new StateMachineException(e);
        }
    }
}
