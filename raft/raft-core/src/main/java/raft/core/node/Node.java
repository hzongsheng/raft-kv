package raft.core.node;

import raft.core.log.statemachine.StateMachine;
import raft.core.node.role.RoleNameAndLeaderId;

public interface Node {

    void registerStateMachine(StateMachine stateMachine);

    RoleNameAndLeaderId getRoleNameAndLeaderId();

    void start();

    void appendLog(byte[] commandBytes);

    void stop() throws InterruptedException;

}
