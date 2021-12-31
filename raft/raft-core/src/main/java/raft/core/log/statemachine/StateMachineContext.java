package raft.core.log.statemachine;

public interface StateMachineContext {

    void generateSnapshot(int lastIncludeIndex);
}
