package raft.core.log;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 可以通过给定 baseDir 和 lastIncludedIndex 创建分代目录 {baseDir}.{lastIncludedIndex}
 * 并在此目录之下创建索引文件和日志文件 {base}/log-{index}/entries.bin , .../entries.idx
 */
class LogGeneration extends AbstractLogDir implements Comparable<LogGeneration> {

    private static final Pattern DIR_NAME_PATTERN = Pattern.compile("log-(\\d+)");
    private final int lastIncludedIndex;

    /**
     * 给定一个 baseDir , 将在这个根目录下创建 log-{lastIncludeIndex} 分代目录
     * super 调用父类初始化, 将在这个 log-{lastIncludeIndex} 下创建日志文件和索引文件
     *
     *
     * @param baseDir log 根目录
     * @param lastIncludedIndex 最后一个 log index
     */
    LogGeneration(File baseDir, int lastIncludedIndex) {
        super(new File(baseDir, generateDirName(lastIncludedIndex)));
        this.lastIncludedIndex = lastIncludedIndex;
    }

    /**
     * 通过全名 xx.xx.log-xx 创建 LogGeneration
     *
     * @param dir 目录全名
     */
    LogGeneration(File dir) {
        super(dir);
        // getName 只是返回路径最后一个名字, 不是全名
        Matcher matcher = DIR_NAME_PATTERN.matcher(dir.getName());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("not a directory name of log generation, [" + dir.getName() + "]");
        }
        lastIncludedIndex = Integer.parseInt(matcher.group(1));
    }

    /**
     * 验证名称是否符合 log-数字 的形式
     *
     * @param dirName 目录名称
     * @return 是否匹配
     */
    static boolean isValidDirName(String dirName) {
        return DIR_NAME_PATTERN.matcher(dirName).matches();
    }

    /**
     * 根据 index 生成 log-index 的目录名
     * @param lastIncludedIndex index
     * @return string
     */
    private static String generateDirName(int lastIncludedIndex) {
        return "log-" + lastIncludedIndex;
    }

    public int getLastIncludedIndex() {
        return lastIncludedIndex;
    }

    @Override
    public int compareTo(LogGeneration o) {
        return Integer.compare(lastIncludedIndex, o.lastIncludedIndex);
    }

    @Override
    public String toString() {
        return "LogGeneration{" +
                "dir=" + dir +
                ", lastIncludedIndex=" + lastIncludedIndex +
                '}';
    }
}
