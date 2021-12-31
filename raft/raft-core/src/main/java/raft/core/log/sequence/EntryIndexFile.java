package raft.core.log.sequence;

import raft.core.support.RandomAccessFileAdapter;
import raft.core.support.SeekableFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EntryIndexFile implements Iterable<EntryIndexItem>{

    private static final long OFFSET_MAX_ENTRY_INDEX = Integer.BYTES;
    /**
     * 一个 entry 的长度:
     * offset       kind    term
     * long(8)      int(4)  int(4)  = 16
     */
    private static final int LENGTH_ENTRY_INDEX_ITEM = 16;

    /**
     *  索引总数 */
    private int entryIndexCount;
    private int minEntryIndex;
    private int maxEntryIndex;

    // 索引文件
    private final SeekableFile seekableFile;
    // 索引缓冲
    private Map<Integer, EntryIndexItem> entryIndexMap = new HashMap<>();


    public EntryIndexFile(File file) throws IOException {
        this(new RandomAccessFileAdapter(file));
    }

    public EntryIndexFile(SeekableFile seekableFile) throws IOException {
        this.seekableFile = seekableFile;
        load();
    }

    private void load() throws  IOException{
        if (seekableFile.size() == 0L) {
            entryIndexCount = 0;
            return;
        }

        /* entryIndexFIle 结构:
         * minEntryIndex | maxEntryIndex
         * offset | kind | term |
         * ...
         */
        minEntryIndex = seekableFile.readInt();
        maxEntryIndex = seekableFile.readInt();
        updateEntryIndexCount();

        long offset;
        int kind;
        int term;
        for (int i = minEntryIndex; i <= maxEntryIndex; i++) {
            offset = seekableFile.readLong();
            kind = seekableFile.readInt();
            term = seekableFile.readInt();
            entryIndexMap.put(i, new EntryIndexItem(i, offset, kind, term));
        }
    }

    private void updateEntryIndexCount(){
        entryIndexCount = maxEntryIndex - minEntryIndex + 1;
    }

    public boolean isEmpty(){return entryIndexCount == 0;}

    public int getMinEntryIndex() { return minEntryIndex; }

    public int getMaxEntryIndex() { return maxEntryIndex; }

    public void checkEmpty(){
        if(isEmpty()){
            throw new IllegalStateException("no entry index");
        }
    }

    public SeekableFile getSeekableFile() {
        return seekableFile;
    }

    public void appendEntryIndex(int index, long offset, int kind, int term) throws IOException{

        // 由于 entryIndex 同时包含了索引文件和索引缓冲, 所以写入操作需要同时考虑到这两者
        if(seekableFile.size() != 0L) {

            if (index != maxEntryIndex + 1) {
                throw new IllegalArgumentException(
                        "index must be " + (maxEntryIndex + 1) + ", but was " + index);
            }
            seekableFile.seek(OFFSET_MAX_ENTRY_INDEX);
        }else {
            // 文件为空, 则写入 minEntryIndex
            seekableFile.writeInt(index);
            minEntryIndex = index;
        }

        // 覆盖写 max index, 写文件和写缓存
        seekableFile.writeInt(index);
        maxEntryIndex = index;
        updateEntryIndexCount();

        // 之后追加写 entry 具体信息
        // 1. 写文件
        seekableFile.seek(getOffsetOfEntryIndexItem(index));
        seekableFile.writeLong(offset);
        seekableFile.writeInt(kind);
        seekableFile.writeInt(term);

        // 2. 写缓存
        entryIndexMap.put(index, new EntryIndexItem(index, offset, kind, term));

    }

    private long getOffsetOfEntryIndexItem(int index){
        return (long) (index - minEntryIndex) * LENGTH_ENTRY_INDEX_ITEM + Integer.BYTES * 2;
    }

    public void clear() throws IOException{
        seekableFile.truncate(0L);
        entryIndexCount = 0;
        entryIndexMap.clear();
    }

    public void removeAfter(int newMaxEntryIndex) throws IOException{
        if(isEmpty() || newMaxEntryIndex >= maxEntryIndex){return;}

        if (newMaxEntryIndex < minEntryIndex) {
            clear();
            return;
        }
        seekableFile.seek(OFFSET_MAX_ENTRY_INDEX);
        seekableFile.writeInt(newMaxEntryIndex);
        seekableFile.truncate(getOffsetOfEntryIndexItem(newMaxEntryIndex + 1));
        for (int i = newMaxEntryIndex + 1; i <= maxEntryIndex; i++) {
            entryIndexMap.remove(i);
        }
        maxEntryIndex = newMaxEntryIndex;
        entryIndexCount = newMaxEntryIndex - minEntryIndex + 1;
    }

    public long getOffset(int entryIndex) {
        return get(entryIndex).getOffset();
    }

    public EntryIndexItem get(int entryIndex){
        checkEmpty();
        if (entryIndex < minEntryIndex || entryIndex > maxEntryIndex) {
            throw new IllegalArgumentException("index < min or index > max");
        }
        return entryIndexMap.get(entryIndex);
    }

    @Override
    public Iterator<EntryIndexItem> iterator(){
        if(isEmpty()){
            return Collections.emptyIterator();
        }
        return new EntryIndexIterator(entryIndexCount, minEntryIndex);
    }

    private class EntryIndexIterator implements Iterator<EntryIndexItem>{

        /**
         * 记录遍历时 index 的数目, fail-fast */
        private final int entryIndexCount;
        private int currentEntryIndex;

        EntryIndexIterator(int entryIndexCount, int minEntryIndex) {
            this.entryIndexCount = entryIndexCount;
            this.currentEntryIndex = minEntryIndex;
        }

        @Override
        public boolean hasNext() {
            checkModification();
            return currentEntryIndex <= maxEntryIndex;
        }

        /**
         * 检测在遍历过程中 索引数目 是否发生改变
         */
        private void checkModification() {
            if (this.entryIndexCount != EntryIndexFile.this.entryIndexCount) {
                throw new IllegalStateException("entry index count changed");
            }
        }

        @Override
        public EntryIndexItem next() {
            checkModification();
            return entryIndexMap.get(currentEntryIndex++);
        }
    }

    public void close() throws IOException{
        seekableFile.close();
    }

}





























