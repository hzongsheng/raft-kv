package raft.core.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 相当于是 LogGeneration 的包装类, 能够根据 index 产生 LogGeneration 对象, 返回最新 LogGeneration 对象等
 */
public class RootDir {
    private static final Logger logger = LoggerFactory.getLogger(RootDir.class);

    static final String FILE_NAME_SNAPSHOT = "service.ss";
    static final String FILE_NAME_ENTRIES = "entries.bin";
    static final String FILE_NAME_ENTRY_OFFSET_INDEX = "entries.idx";

    private static final String DIR_NAME_GENERATING = "generating";
    private static final String DIR_NAME_INSTALLING = "installing";

    private final File baseDir;

    RootDir(File baseDir) {
        if (!baseDir.exists()) {
            throw new IllegalArgumentException("dir " + baseDir + " not exists");
        }
        this.baseDir = baseDir;
    }

    LogDir getLogDirForGenerating() {
        return getOrCreateNormalLogDir(DIR_NAME_GENERATING);
    }

    LogDir getLogDirForInstalling() {
        return getOrCreateNormalLogDir(DIR_NAME_INSTALLING);
    }

    private NormalLogDir getOrCreateNormalLogDir(String name) {
        NormalLogDir logDir = new NormalLogDir(new File(baseDir, name));
        if (!logDir.exists()) {
            logDir.initialize();
        }
        return logDir;
    }

    LogDir rename(LogDir dir, int lastIncludedIndex) {
        LogGeneration destDir = new LogGeneration(baseDir, lastIncludedIndex);
        if (destDir.exists()) {
            throw new IllegalStateException("failed to rename, dest dir " + destDir + " exists");
        }

        logger.info("rename dir {} to {}", dir, destDir);
        if (!dir.renameTo(destDir)) {
            throw new IllegalStateException("failed to rename " + dir + " to " + destDir);
        }
        return destDir;
    }

    LogGeneration createFirstGeneration() {
        LogGeneration generation = new LogGeneration(baseDir, 0);
        generation.initialize();
        return generation;
    }

    /**
     * 得到最新的日志 LogGeneration 对象
     *
     * @return 最新日志 LogGeneration 对象
     */
    LogGeneration getLatestGeneration() {
        File[] files = baseDir.listFiles();
        if (files == null) {
//            return null;
            return createFirstGeneration();
        }
        LogGeneration latest = null;
        String fileName;
        LogGeneration generation;
        for (File file : files) {
            if (!file.isDirectory()) {
                continue;
            }
            fileName = file.getName();
            if (DIR_NAME_GENERATING.equals(fileName) || DIR_NAME_INSTALLING.equals(fileName) ||
                    !LogGeneration.isValidDirName(fileName)) {
                continue;
            }
            // 根据目录生成 LogGeneration 对象, 如果比当前 latest generation 还要年轻的话就用此替代 latest
            generation = new LogGeneration(file);
            if (latest == null || generation.compareTo(latest) > 0) {
                latest = generation;
            }
        }
        return latest;
    }

}
