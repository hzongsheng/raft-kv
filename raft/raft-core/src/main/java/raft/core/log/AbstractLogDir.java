package raft.core.log;

import raft.core.support.Files;

import java.io.File;
import java.io.IOException;

/**
 * File 的包装类, 在构造器给定的 dir 下创建索引文件和日志文件
 */
public class AbstractLogDir implements LogDir{
    final File dir;

    public AbstractLogDir(File dir) { this.dir = dir; }

    /**
     * 创建给定的路径下日志文件和索引文件
     */
    @Override
    public void initialize() {
        if (!dir.exists() && !dir.mkdir()) {
            throw new LogException("failed to create directory " + dir);
        }
        try {
            // 尝试创建日志文件和索引文件
            Files.touch(getEntriesFile());
            Files.touch(getEntryOffsetIndexFile());
        } catch (IOException e) {
            throw new LogException("failed to create file", e);
        }
    }

    @Override
    public boolean exists() {
        return dir.exists();
    }

    @Override
    public File getSnapshotFile() {
        return new File(dir, RootDir.FILE_NAME_SNAPSHOT);
    }

    @Override
    public File getEntriesFile() {
        return new File(dir, RootDir.FILE_NAME_ENTRIES);
    }

    @Override
    public File getEntryOffsetIndexFile() {
        return new File(dir, RootDir.FILE_NAME_ENTRY_OFFSET_INDEX);
    }

    @Override
    public File get() {
        return dir;
    }

    @Override
    public boolean renameTo(LogDir logDir) {
        return dir.renameTo(logDir.get());
    }

}
