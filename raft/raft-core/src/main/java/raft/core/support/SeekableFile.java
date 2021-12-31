package raft.core.support;

import java.io.IOException;
import java.io.InputStream;

public interface SeekableFile {
    // 获取当前位置

    long position() throws IOException;

    // 移动到指定位置

    void seek(long position) throws IOException;

    // 写入 int, long, byte数组

    void writeInt(int i) throws IOException;

    void writeLong(long l) throws IOException;

    void write(byte[] b) throws IOException;

    // 读 int , long, byte数组

    int readInt() throws IOException;

    long readLong() throws IOException;

    int read(byte[] b) throws IOException;

    // 文件大小

    long size() throws IOException;

    // 裁剪到指定大小

    void truncate(long size) throws IOException;

    // 从指定输入的流

    InputStream inputStream(long start) throws IOException;

    // 刷到磁盘

    void flush() throws IOException;

    void close() throws IOException;
}
