package raft.core.log;

import com.google.common.eventbus.EventBus;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import java.util.*;

import raft.core.log.entry.Entry;
import raft.core.log.entry.EntryMeta;
import raft.core.log.entry.GeneralEntry;
import raft.core.log.entry.NoOpEntry;
import raft.core.log.sequence.EntrySequence;
import raft.core.log.statemachine.StateMachine;
import raft.core.log.statemachine.StateMachineContext;
import raft.core.node.NodeId;
import raft.core.rpc.message.AppendEntriesRpc;

public abstract class AbstractLog implements Log{
    private static final Logger logger = LoggerFactory.getLogger(AbstractLog.class);

    /**
     * 逻辑委托给 entrySequence, 包含一个 stateMachine 用于 apply log
     */
    protected EntrySequence entrySequence;
    protected StateMachine stateMachine;
    protected final StateMachineContext stateMachineContext = new StateMachineContextImpl();
    protected final EventBus eventBus;

    AbstractLog(EventBus eventBus){
        this.eventBus = eventBus;
    }

    @Override
    public AppendEntriesRpc createAppendEntriesRpc(int term, NodeId selfId, int nextIndex, int maxEntries) {
        // 这里的 nextIndex 是下一条要复制的 entry 起点
        int nextLogIndex = entrySequence.getNextLogIndex();
        if (nextIndex > nextLogIndex) {
            throw new IllegalArgumentException("illegal next index " + nextIndex);
        }
        AppendEntriesRpc rpc = new AppendEntriesRpc();
        /* 基础信息 */
        rpc.setMessageId(UUID.randomUUID().toString());
        rpc.setTerm(term);
        rpc.setLeaderId(selfId);

        /* commit 信息*/
        rpc.setLeaderCommit(getCommitIndex());

        /* Entries 信息
         * 如果获得的 entry 为 null 那么 rpc 的 Entries 为空的 Collection */
        Entry entry = entrySequence.getEntry(nextIndex - 1);
        if(entry != null) {
            rpc.setPrevLogIndex(entry.getIndex());
            rpc.setPrevLogTerm(entry.getTerm());
        }else{
            rpc.setPrevLogIndex(-1);
            rpc.setPrevLogTerm(term);
        }

        if (!entrySequence.isEmpty()) {
            // 如果没有指定 maxEntries 的话, 那么最多就是一条 entry
            // Entries 没有指定的话默认为空的 Collection
            int maxIndex = (maxEntries == ALL_ENTRIES ? nextLogIndex : Math.min(nextLogIndex, nextIndex + maxEntries));
            rpc.setEntries(entrySequence.subList(nextIndex, maxIndex));
        }
        else {
            rpc.setEntries(Collections.emptyList());
        }
        return rpc;
    }

    @Override
    public int getNextIndex() {
        return entrySequence.getNextLogIndex();
    }

    @Override
    public int getCommitIndex() {
        return entrySequence.getCommitIndex();
    }

    @Override
    public boolean isNewerThan(int lastLogIndex, int lastLogTerm) {
        EntryMeta lastEntryMeta = getLastEntryMeta();
        logger.debug("last entry ({}, {}), candidate ({}, {})", lastEntryMeta.getIndex(), lastEntryMeta.getTerm(), lastLogIndex, lastLogTerm);
        return lastEntryMeta.getTerm() > lastLogTerm || lastEntryMeta.getIndex() > lastLogIndex;
    }

    @Override
    public EntryMeta getLastEntryMeta() {
        if (entrySequence.isEmpty()) {
            return new EntryMeta(Entry.KIND_NO_OP, 0,0);
        }
        return entrySequence.getLastEntry().getMeta();
    }

    /**
     * 添加 No-Op 的 entry
     */
    @Override
    public NoOpEntry appendEntry(int term) {
        NoOpEntry entry = new NoOpEntry(entrySequence.getNextLogIndex(), term);
        entrySequence.append(entry);
        return entry;
    }

    /**
     * 添加 普通的 entry (带命令)
     */
    @Override
    public GeneralEntry appendEntry(int term, byte[] command) {
        GeneralEntry entry = new GeneralEntry(entrySequence.getNextLogIndex(), term, command);
        entrySequence.append(entry);
        return entry;
    }

    @Override
    public boolean appendEntriesFromLeader(int prevLogIndex, int prevLogTerm, List<Entry> leaderEntries) {
        // 心跳消息
        if (leaderEntries.isEmpty()) {
            return true;
        }

//      检查前一条日志是否匹配
        if (!checkIfPreviousLogMatches(prevLogIndex, prevLogTerm)) {
            return false;
        }

        assert prevLogIndex + 1 == leaderEntries.get(0).getIndex();
        EntrySequenceView newEntries = removeUnmatchedLog(new EntrySequenceView(leaderEntries));
        appendEntriesFromLeader(newEntries);
        return true;
    }

    private void appendEntriesFromLeader(EntrySequenceView leaderEntries) {
        // leader 发来的 Entries 为空, 说明是心跳包
        if (leaderEntries.isEmpty()) {
            return;
        }
        logger.debug(String.format("append entries from leader from %s to %s", leaderEntries.getFirstLogIndex(), leaderEntries.getLastLogIndex()));
        for (Entry leaderEntry : leaderEntries) {
            entrySequence.append(leaderEntry);
        }
    }

    private boolean checkIfPreviousLogMatches(int prevLogIndex, int prevLogTerm) {
        if(prevLogIndex == -1 && entrySequence.isEmpty()) {
            return true;
        }
        EntryMeta meta = entrySequence.getEntryMeta(prevLogIndex);
        if (meta == null) {
            logger.debug(String.format("previous log %s not found", prevLogIndex));
            return false;
        }
        int term = meta.getTerm();
        if (term != prevLogTerm) {
            logger.debug(String.format("different term of previous log, local %s, remote %s", term, prevLogTerm));
            return false;
        }
        return true;
    }

    /**
     * 通过一个 entry 数组构造一个 entrySequence 的视图
     * 视图指的是提供一个相同操作的界面, 但实际上只是一个 entry 数组的包装
     */
    private static class EntrySequenceView implements Iterable<Entry> {

        private final List<Entry> entries;
        private int firstLogIndex = -1;
        private int lastLogIndex = -1;

        EntrySequenceView(List<Entry> entries) {
            this.entries = entries;
            if (!entries.isEmpty()) {
                firstLogIndex = entries.get(0).getIndex();
                lastLogIndex = entries.get(entries.size() - 1).getIndex();
            }
        }

        Entry get(int index) {
            if (entries.isEmpty() || index < firstLogIndex || index > lastLogIndex) {
                return null;
            }
            return entries.get(index - firstLogIndex);
        }

        boolean isEmpty() {
            return entries.isEmpty();
        }

        int getFirstLogIndex() {
            return firstLogIndex;
        }

        int getLastLogIndex() {
            return lastLogIndex;
        }

        /**
         * 获取子视图
         * @param fromIndex from index
         * @return 子视图
         */
        EntrySequenceView subView(int fromIndex) {
            if (entries.isEmpty() || fromIndex > lastLogIndex) {
                return new EntrySequenceView(Collections.emptyList());
            }
            return new EntrySequenceView(
                    entries.subList(fromIndex - firstLogIndex, entries.size())
            );
        }

        @Override
        public Iterator<Entry> iterator() {
            return entries.iterator();
        }
    }

    private EntrySequenceView removeUnmatchedLog(EntrySequenceView leaderEntries) {
        assert !leaderEntries.isEmpty();
        int firstUnmatched = findFirstUnmatchedLog(leaderEntries);
        if(firstUnmatched < 0){
            return new EntrySequenceView(Collections.emptyList());
        }
        removeEntriesAfter(firstUnmatched - 1);
        return leaderEntries.subView(firstUnmatched);
    }

    /**
     * 从前往后找, 直到不匹配
     * @param leaderEntries leader entries 的 view
     * @return int index
     */
    private int findFirstUnmatchedLog(EntrySequenceView leaderEntries) {
        assert !leaderEntries.isEmpty();
        int logIndex;
        EntryMeta followerEntryMeta;
        for (Entry leaderEntry : leaderEntries) {
            logIndex = leaderEntry.getIndex();
            followerEntryMeta = entrySequence.getEntryMeta(logIndex);
            if (followerEntryMeta == null || followerEntryMeta.getTerm() != leaderEntry.getTerm()) {
                return logIndex;
            }
        }
        return leaderEntries.getLastLogIndex() + 1;
    }

    private void removeEntriesAfter(int index){
        if(entrySequence.isEmpty() || index >= entrySequence.getLastLogIndex()){
            return;
        }

        logger.debug(String.format("remove entries after %s", index));
        entrySequence.removeAfter(index);
    }

    @Override
    public void advanceCommitIndex(int newCommitIndex, int currentTerm) {
        if (!validateNewCommitIndex(newCommitIndex, currentTerm)) {
            return;
        }
        logger.debug("advance commit index from {} to {}", getCommitIndex(), newCommitIndex);
        entrySequence.commit(newCommitIndex);

        advanceApplyIndex();
    }


    private boolean validateNewCommitIndex(int newCommitIndex, int currentTerm) {
        if (newCommitIndex <= getCommitIndex()) {
            return false;
        }

        EntryMeta meta = entrySequence.getEntryMeta(newCommitIndex);
        if (meta == null) {
            logger.debug("log of new commit index {} not found", newCommitIndex);
            return false;
        }
        if (meta.getTerm() != currentTerm) {
            logger.debug("log term of new commit index != current term ({} != {})", meta.getTerm(), currentTerm);
            return false;
        }
        return true;
    }
    private void advanceApplyIndex() {
        // start up and snapshot exists
        int lastApplied = stateMachine.getLastApplied();
        for (Entry entry : entrySequence.subList(lastApplied + 1, getCommitIndex() + 1)) {
            applyEntry(entry);
        }
    }

    private void applyEntry(Entry entry) {
        // skip no-op entry and membership-change entry
        if (isApplicable(entry)) {
            stateMachine.applyLog(stateMachineContext, entry.getIndex(), entry.getCommandBytes(), entrySequence.getFirstLogIndex());
        }
    }

    private boolean isApplicable(Entry entry) {
        // 普通日志
        return entry.getKind() == Entry.KIND_GENERAL;
    }


    private class StateMachineContextImpl implements StateMachineContext {

        @Override
        public void generateSnapshot(int lastIncludedIndex) {
//            eventBus.post(new SnapshotGenerateEvent(lastIncludedIndex));
        }

    }

    @Override
    public void setStateMachine(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    @Override
    public void close() {
        entrySequence.close();
    }

}



























































