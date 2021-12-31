package raft.core.log.sequence;

import raft.core.log.entry.Entry;

import java.util.ArrayList;
import java.util.List;

public class MemoryEntrySequence extends AbstractEntrySequence{

    private final List<Entry> entries = new ArrayList<>();
    private int commitIndex = 0;

    /**
     * 默认构造器, logIndexOffset 开始于 1 */
    public MemoryEntrySequence(){ this(1); }

    /**
     * 适用于 snapshot 等当前日志开始的 offset 不为 1 的情况 */
    public MemoryEntrySequence(int logIndexOffset){ super(logIndexOffset); }

    @Override
    protected List<Entry> doSubList(int formIndex, int toIndex){
        return entries.subList(formIndex - logIndexOffset, toIndex - logIndexOffset);
    }

    @Override
    protected Entry doGetEntry(int index) {
        return entries.get(index - logIndexOffset);
    }

    @Override
    protected void doAppend(Entry entry) {
        entries.add(entry);
    }

    @Override
    public void commit(int index) {
        commitIndex = index;
    }

    @Override
    public int getCommitIndex() {
        return commitIndex;
    }

    @Override
    protected void doRemoveAfter(int index) {
        if(index < doGetFirstLogIndex()){
            entries.clear();
            nextLogIndex = logIndexOffset;
        } else{
            entries.subList(index - logIndexOffset + 1, entries.size()).clear();
            nextLogIndex = index + 1;
        }
    }

    @Override
    public void close(){
    }

    @Override
    public String toString() {
        return "MemoryEntrySequence{" +
                "logIndexOffset=" + logIndexOffset +
                ", nextLogIndex=" + nextLogIndex +
                ", entries.size=" + entries.size() +
                '}';
    }
}
