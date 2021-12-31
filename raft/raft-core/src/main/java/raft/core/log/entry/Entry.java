package raft.core.log.entry;

public interface Entry {
    // 日志类型
    int KIND_NO_OP = 0;
    int KIND_GENERAL = 1;

    int getKind();

    int getIndex();

    int getTerm();

    EntryMeta getMeta();

    byte[] getCommandBytes();

}