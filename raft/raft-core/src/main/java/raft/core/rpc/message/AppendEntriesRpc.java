package raft.core.rpc.message;

import raft.core.log.entry.Entry;
import raft.core.node.NodeId;

import java.util.Collections;
import java.util.List;

public class AppendEntriesRpc {
    /**
     * appendEntriesRpc 的消息序列号
     * term 选举 term
     * leaderId leader 节点自己的 ID
     * prevLogIndex 前一条日志的索引
     * PrvLogTerm 前一条日志的 term
     * entries 复制的日志条目
     * leaderCommit leader 的 commitIndex
     */
    private String messageId;
    private int term;
    private NodeId leaderId;
    private int prevLogIndex;
    private int prevLogTerm;
    private List<Entry> entries;
    private int leaderCommit;


    public void setTerm(int term) {
        this.term = term;
    }

    public void setLeaderId(NodeId leaderId) {
        this.leaderId = leaderId;
    }

    public void setPrevLogIndex(int prevLogIndex) {
        this.prevLogIndex = prevLogIndex;
    }

    public void setPrevLogTerm(int prevLogTerm) {
        this.prevLogTerm = prevLogTerm;
    }

    public void setLeaderCommit(int leaderCommit) {
        this.leaderCommit = leaderCommit;
    }

    public NodeId getLeaderId() {
        return leaderId;
    }

    public int getTerm() {
        return term;
    }

    public int getPrevLogIndex() {
        return prevLogIndex;
    }

    public int getPrevLogTerm() {
        return prevLogTerm;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public int getLeaderCommit() {
        return leaderCommit;
    }

    public int getLastEntryIndex(){
        return this.entries.isEmpty() ? this.prevLogIndex : this.entries.get(this.entries.size() - 1).getIndex();
    }

    public String getMessageId() { return messageId; }

    @Override
    public String toString() {
        return "AppendEntriesRpc{" +
                ", entries.size=" + entries.size() +
                ", leaderCommit=" + leaderCommit +
                ", leaderId=" + leaderId +
                ", prevLogIndex=" + prevLogIndex +
                ", prevLogTerm=" + prevLogTerm +
                "term=" + term +
                '}';
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

}
