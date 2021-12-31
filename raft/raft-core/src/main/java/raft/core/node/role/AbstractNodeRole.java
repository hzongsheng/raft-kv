package raft.core.node.role;

import raft.core.node.NodeId;

public abstract class AbstractNodeRole {
    private final RoleName name;
    protected final int term;

    // constructor
    AbstractNodeRole(RoleName name, int term){
        this.name = name;
        this.term = term;
    }

    public RoleName getName(){
        return name;
    }

    public int getTerm(){ return term; }

    public abstract NodeId getLeaderId(NodeId selfId);

    public RoleNameAndLeaderId getNameAndLeaderId(NodeId selfId){
        return new RoleNameAndLeaderId(name, getLeaderId(selfId));
    }

    // 取消当前定时器
    public abstract void cancelTimeoutOrTask();

}
