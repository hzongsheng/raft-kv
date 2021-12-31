package raft.core.log;

import com.google.common.eventbus.EventBus;
import raft.core.log.sequence.FileEntrySequence;

import java.io.File;

public class FileLog extends AbstractLog{

    private final RootDir rootDir;

    public FileLog(File baseDir, EventBus eventBus) {
        super(eventBus);
        rootDir = new RootDir(baseDir);

        LogGeneration latestGeneration = rootDir.getLatestGeneration();
        if (latestGeneration != null) {
            entrySequence = new FileEntrySequence(latestGeneration, latestGeneration.getLastIncludedIndex());
        } else {
            // 日志不存在, 创建第一代 log
            LogGeneration firstGeneration = rootDir.createFirstGeneration();
            entrySequence = new FileEntrySequence(firstGeneration, 0);
        }
    }
}
