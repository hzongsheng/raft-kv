package raft.core.log.sequence;

import raft.core.log.LogDir;
import raft.core.log.LogException;
import raft.core.log.entry.Entry;
import raft.core.log.entry.EntryMeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class FileEntrySequence extends AbstractEntrySequence{
    private final EntryFactory entryFactory = new EntryFactory();

    /**
     * entry 存储文件 */
    private final EntriesFile entriesFile;
    /**
     * entry 索引文件 */
    private final EntryIndexFile entryIndexFile;
    /**
     * entry 缓存 */
    private final LinkedList<Entry> pendingEntries = new LinkedList<>();

    /**
     * commitIndex 记录 */
    private int commitIndex;

    public FileEntrySequence(LogDir logDir, int logIndexOffset) {
        super(logIndexOffset);
        try {
            this.entriesFile = new EntriesFile(logDir.getEntriesFile());
            this.entryIndexFile = new EntryIndexFile(logDir.getEntryOffsetIndexFile());
            initialize();
        } catch (IOException e) {
            throw new LogException("failed to open entries file or entry index file", e);
        }
    }

    public FileEntrySequence(EntriesFile entriesFile, EntryIndexFile entryIndexFile, int logIndexOffset) {
        super(logIndexOffset);
        this.entriesFile = entriesFile;
        this.entryIndexFile = entryIndexFile;
        initialize();
    }

    private void initialize() {
        if (entryIndexFile.isEmpty()) {
            commitIndex = logIndexOffset - 1;
            return;
        }
        // 父类字段
        logIndexOffset = entryIndexFile.getMinEntryIndex();
        nextLogIndex = entryIndexFile.getMaxEntryIndex() + 1;

        commitIndex = entryIndexFile.getMaxEntryIndex();
    }

    /**
     * 包含 from 不包含 to
     * @param fromIndex fromIndex 包含
     * @param toIndex toIndex 不包含
     */
    @Override
    protected List<Entry> doSubList(int fromIndex, int toIndex) {
        List<Entry> result = new ArrayList<>();

        // 从文件中获取
        if (!entryIndexFile.isEmpty() && fromIndex <= entryIndexFile.getMaxEntryIndex()) {
            int maxIndex = Math.min(entryIndexFile.getMaxEntryIndex() + 1, toIndex);
            for (int i = fromIndex; i < maxIndex; i++) {
                result.add(getEntryInFile(i));
            }
        }

        // 从缓存获取
        if (!pendingEntries.isEmpty() && fromIndex <= pendingEntries.getFirst().getIndex() && toIndex > pendingEntries.getLast().getIndex()) {
            Iterator<Entry> iterator = pendingEntries.iterator();
            Entry entry;
            int index;
            while (iterator.hasNext()) {
                entry = iterator.next();
                index = entry.getIndex();
                if (fromIndex <= index && index < toIndex) {
                    result.add(entry);
                }
            }
        }
        return result;
    }

    @Override
    protected Entry doGetEntry(int index) {
        // 从缓冲中获取
        if (!pendingEntries.isEmpty()) {
            int firstPendingEntryIndex = pendingEntries.getFirst().getIndex();
            if (index >= firstPendingEntryIndex) {
                return pendingEntries.get(index - firstPendingEntryIndex);
            }
        }

        // 从文件中获取
        assert !entryIndexFile.isEmpty();
        return getEntryInFile(index);
    }

    private Entry getEntryInFile(int index) {
        // index 查 offset
        long offset = entryIndexFile.getOffset(index);
        // 从文件中读 entry
        try {
            return entriesFile.loadEntry(offset, entryFactory);
        } catch (IOException e) {
            throw new LogException("failed to load entry " + index, e);
        }
    }

    @Override
    public EntryMeta getEntryMeta(int index) {
        if (!isEntryPresent(index)) {
            return null;
        }
        EntryMeta result = null;
        try {
            result = pendingEntries.get(index - getFirstPendingEntryIndex()).getMeta();
        } catch(Exception e) {
            result = entryIndexFile.get(index).toEntryMeta();
        }
        return result;

    }

    private int getFirstPendingEntryIndex(){
        if(pendingEntries.isEmpty()) {
            throw new LogException("pendingEntries is empty");
        }
        return pendingEntries.get(0).getIndex();
    }

    /**
     * 得到最后一个 entry
     */
    @Override
    public Entry getLastEntry() {
        if (isEmpty()) {
            return null;
        }
        if (!pendingEntries.isEmpty()) {
            return pendingEntries.getLast();
        }
        assert !entryIndexFile.isEmpty();
        return getEntryInFile(entryIndexFile.getMaxEntryIndex());
    }

    /**
     *  追加逻辑: 先放到 pendingEntries 中,
     *  之后当 commit 的时候再将 pendingEntries 的内容写到文件中. */
    @Override
    protected void doAppend(Entry entry) {
        pendingEntries.add(entry);
    }

    @Override
    public void commit(int index) {
        // 提交比小于当前 commit 还小的值, 抛出异常
        if (index < commitIndex) {
            throw new IllegalArgumentException("commit index < " + commitIndex);
        }
        // 大于当前已经存储的最大的 entry 的 index, 抛出异常
        if (pendingEntries.isEmpty() || pendingEntries.getLast().getIndex() < index) {
            throw new IllegalArgumentException("no entry to commit or commit index exceed");
        }
        if (index == commitIndex) { return; }

        long offset;
        Entry entry = null;
        try {
            /* commit 的 index 之后的都从缓存中刷到文件中
             * 并写索引 */
            for (int i = commitIndex + 1; i <= index; i++) {
                entry = pendingEntries.removeFirst();
                offset = entriesFile.appendEntry(entry);
                entryIndexFile.appendEntryIndex(i, offset, entry.getKind(), entry.getTerm());
                commitIndex = i;
            }
        } catch (IOException e) {
            throw new LogException("failed to commit entry " + entry, e);
        }
    }

    @Override
    protected void doRemoveAfter(int index) {
        // 所删除的 index 处于 pendingEntries 中
        if (!pendingEntries.isEmpty() && index >= pendingEntries.getFirst().getIndex() - 1) {
            for (int i = index + 1; i <= doGetLastLogIndex(); i++) {
                pendingEntries.removeLast();
            }
            // 注意需要更新 nextLogIndex
            nextLogIndex = index + 1;
            return;
        }
        try {
            if (index >= doGetFirstLogIndex()) {
                // 如果删除起点在文件中, 那么删除所有的 缓冲 中的 entry
                pendingEntries.clear();

                // 并裁剪文件, 索引文件
                entriesFile.truncate(entryIndexFile.getOffset(index + 1));
                entryIndexFile.removeAfter(index);

                nextLogIndex = index + 1;
                commitIndex = index;
            } else {
                // index 小于文件中的最小 index, 则删除文件中, 缓冲中的所有
                pendingEntries.clear();
                entriesFile.clear();
                entryIndexFile.clear();
                nextLogIndex = logIndexOffset;
                commitIndex = logIndexOffset - 1;
            }
        } catch (IOException e) {
            throw new LogException(e);
        }
    }

    @Override
    public int getCommitIndex() { return commitIndex; }

    @Override
    public void close() {
        try {
            entriesFile.close();
            entryIndexFile.close();
        } catch (IOException e) {
            throw new LogException("failed to close", e);
        }
    }

}
