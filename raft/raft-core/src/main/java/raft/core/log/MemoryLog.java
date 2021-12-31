package raft.core.log;

import com.google.common.eventbus.EventBus;
import raft.core.log.sequence.EntrySequence;
import raft.core.log.sequence.MemoryEntrySequence;

public class MemoryLog extends AbstractLog{
    public MemoryLog(EventBus eventBus) {
        this(new MemoryEntrySequence(), eventBus);
    }

    MemoryLog(EntrySequence entrySequence, EventBus eventBus){
        super(eventBus);
        this.entrySequence = entrySequence;

    }
}
