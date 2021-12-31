package raft.core.log.sequence;

import raft.core.log.entry.Entry;
import raft.core.log.entry.EntryMeta;

import java.util.Collections;
import java.util.List;

public abstract class AbstractEntrySequence implements EntrySequence{

    int logIndexOffset;
    // 默认为
    int nextLogIndex;

    public AbstractEntrySequence(int logIndexOffset) {
        this.logIndexOffset = logIndexOffset;
        this.nextLogIndex = logIndexOffset;
    }

    /**
     * 当 logIndexOffset 和 nextLogIndex 一致时候表示当前为空 */
    @Override
    public boolean isEmpty(){
        return logIndexOffset == nextLogIndex;
    }

    @Override
    public int getFirstLogIndex(){
        if(isEmpty()){
            throw new EmptySequenceException();
        }
        return doGetFirstLogIndex();
    }

    int doGetFirstLogIndex(){
        return logIndexOffset;
    }

    @Override
    public int getLastLogIndex() {
        if (isEmpty()) {
            throw new EmptySequenceException();
        }
        return doGetLastLogIndex();
    }

    int doGetLastLogIndex(){
        return  nextLogIndex - 1;
    }

    @Override
    public int getNextLogIndex(){
        return nextLogIndex;
    }

    @Override
    public boolean isEntryPresent(int index){
        return !isEmpty() && index >= doGetFirstLogIndex() && index <= doGetLastLogIndex();
    }

    @Override
    public Entry getEntry(int index){
        if(!isEntryPresent(index)){
            return null;
        }
        return doGetEntry(index);
    }

    protected abstract Entry doGetEntry(int index);

    @Override
    public EntryMeta getEntryMeta(int index) {
        Entry entry = getEntry(index);
        return entry != null ? entry.getMeta() : null;
    }

    @Override
    public Entry getLastEntry(){
        return isEmpty() ? null : doGetEntry(doGetLastLogIndex());
    }

    @Override
    public List<Entry> subList(int fromIndex) {
        if(isEmpty() || fromIndex > doGetLastLogIndex()){
            return Collections.emptyList();
        }

        return subList(Math.max(fromIndex, doGetFirstLogIndex()), nextLogIndex);
    }

    @Override
    public List<Entry> subList(int fromIndex, int toIndex){
        if(isEmpty()){
            throw new EmptySequenceException();
        }

        if(fromIndex < doGetFirstLogIndex() || toIndex > doGetLastLogIndex() + 1 || fromIndex > toIndex){
            throw new IllegalArgumentException("illegal from index " + fromIndex + " or to index" + toIndex);
        }

        return doSubList(fromIndex, toIndex);
    }

    protected abstract List<Entry> doSubList(int fromIndex, int toIndex);

    @Override
    public void append(List<Entry> entries){
        for(Entry entry : entries){
            append(entry);
        }
    }

    @Override
    public void append(Entry entry){
        if(entry.getIndex() != nextLogIndex){
            throw new IllegalArgumentException("entry index must be " + nextLogIndex);
        }

        doAppend(entry);
        // nextlogIndex 更新
        nextLogIndex++;
    }

    protected abstract void doAppend(Entry entry);

    @Override
    public void removeAfter(int index){
        if(isEmpty() || index >= doGetLastLogIndex()){
            return;
        }
        doRemoveAfter(index);
    }

    protected abstract void doRemoveAfter(int index);
}















































