package raft.core.log.sequence;

import raft.core.log.entry.Entry;
import raft.core.log.entry.GeneralEntry;
import raft.core.log.entry.NoOpEntry;

public class EntryFactory {
    public Entry create(int kind, int index, int term, byte[] commandBytes){
        switch (kind){
            case Entry.KIND_GENERAL:
                return new GeneralEntry(index, term, commandBytes);

            case Entry.KIND_NO_OP:
                return new NoOpEntry(index, term);

            default:
                throw new IllegalArgumentException("unexpected entry kind " + kind);
        }
    }
}
