package raft.core.log;

import java.io.File;

/**
 * 和父类 AbstractLogDir 一样
 */
public class NormalLogDir extends AbstractLogDir{
    NormalLogDir(File dir) {
        super(dir);
    }

    @Override
    public String toString() {
        return "NormalLogDir{" +
                "dir=" + dir +
                '}';
    }

}
