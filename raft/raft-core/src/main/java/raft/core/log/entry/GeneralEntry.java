package raft.core.log.entry;

public class GeneralEntry extends AbstractEntry{
    private final byte[] commandBytes;

    public GeneralEntry(int index, int term, byte[] commandBytes) {
        super(Entry.KIND_GENERAL, index, term);
        this.commandBytes = commandBytes;
    }

    @Override
    public byte[] getCommandBytes() {
        return commandBytes;
    }

    @Override
    public String toString() {
        return "GeneralEntry{" +
                "index=" + index +
                ", term=" + term +
                '}';
    }
}
