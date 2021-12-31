package raft.core.log.sequence;

import raft.core.log.entry.Entry;
import raft.core.support.RandomAccessFileAdapter;
import raft.core.support.SeekableFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class EntriesFile {

    private final SeekableFile seekableFile;

    public EntriesFile(SeekableFile seekableFile) {
        this.seekableFile = seekableFile;
    }

    public EntriesFile(File file) throws FileNotFoundException{
        this(new RandomAccessFileAdapter(file));
    }

    public long appendEntry(Entry entry) throws IOException{
        // entry 的结构:
        // kind | index | term | length | command bytes
        long offset = seekableFile.size();
        seekableFile.seek(offset);
        seekableFile.writeInt(entry.getKind());
        seekableFile.writeInt(entry.getIndex());
        seekableFile.writeInt(entry.getTerm());
        byte[] commandBytes = entry.getCommandBytes();
        seekableFile.writeInt(commandBytes.length);
        seekableFile.write(commandBytes);
        return offset;
    }

    /**
     * 给定 index 返回 entry
     */
    public Entry loadEntry(long offset, EntryFactory factory) throws IOException{
        if(offset > seekableFile.size()){
            throw new IllegalArgumentException("offset > size");
        }
        seekableFile.seek(offset);
        int kind = seekableFile.readInt();
        int index = seekableFile.readInt();
        int term = seekableFile.readInt();
        int length = seekableFile.readInt();
        byte[] bytes = new byte[length];
        seekableFile.read(bytes);
        return factory.create(kind, index, term, bytes);
    }

    public long size() throws IOException{ return seekableFile.size(); }

    public void clear() throws IOException {
        truncate(0L);
    }

    public void truncate(long offset) throws IOException {
        seekableFile.truncate(offset);
    }

    public void close() throws IOException {
        seekableFile.close();
    }
}
