package raft.core.log;

import raft.core.log.entry.Entry;
import raft.core.log.entry.EntryMeta;
import raft.core.log.entry.GeneralEntry;
import raft.core.log.entry.NoOpEntry;
import raft.core.log.statemachine.StateMachine;
import raft.core.node.NodeId;
import raft.core.rpc.message.AppendEntriesRpc;

import java.util.List;

public interface Log {
    int ALL_ENTRIES = -1;
    
    EntryMeta getLastEntryMeta();
    
    AppendEntriesRpc createAppendEntriesRpc( int term, NodeId selfId, int nextIndex, int maxEntries);

    /**
     * 获得下个 entry 的index */
    int getNextIndex();

    /** 获得已经 commit 了的 entry
     */
    int getCommitIndex();

    /** 比较两个 log 的新旧
     *
     */
    boolean isNewerThan(int lastLogIndex, int lastLogTerm);

    /**
     * no-op 日志的添加
      */
    NoOpEntry appendEntry(int term);

    /** 普通的日志的添加
     */
    GeneralEntry appendEntry(int term, byte[] command);

    /** 从 leader 接受日志, 并添加
     */
    boolean appendEntriesFromLeader(int prevLogIndex, int prevLogTerm, List<Entry> entries);
    
    void advanceCommitIndex(int newCommitIndex, int currentTerm);

    void setStateMachine(StateMachine stateMachine);

    void close();
}
