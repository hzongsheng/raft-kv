package raft.core.rpc.message;

import raft.core.node.NodeId;

public class RequestVoteRpc {
    /**
     * term 选举 term
     * candidateId 发送者自己的 ID
     * lastLogIndex 候选者最后一条日志的索引
     * lastLogTerm 候选者最后一条日志 term
     */
    private int term;
    private NodeId candidateId;
    private int lastLogIndex = 0;
    private int lastLogTerm = 0;

    public void setTerm(int term) {
        this.term = term;
    }

    public int getTerm() {
        return term;
    }

    public void setCandidateId(NodeId candidateId) {
        this.candidateId = candidateId;
    }

    public NodeId getCandidateId() {
        return candidateId;
    }

    public void setLastLogIndex(int lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    public int getLastLogIndex() {
        return lastLogIndex;
    }

    public void setLastLogTerm(int lastLogTerm) {
        this.lastLogTerm = lastLogTerm;
    }

    public int getLastLogTerm() {
        return lastLogTerm;
    }

    @Override
    public String toString() {
        return "RequestVoteRpc{" +
                "term=" + term +
                ", candidateId=" + candidateId +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                '}';
    }

}
