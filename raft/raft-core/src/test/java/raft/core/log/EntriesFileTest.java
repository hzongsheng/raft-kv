package raft.core.log;

import org.junit.Assert;
import org.junit.Test;
import raft.core.log.entry.GeneralEntry;
import raft.core.log.entry.NoOpEntry;
import raft.core.log.sequence.FileEntrySequence;

import java.io.File;
import java.io.IOException;

public class EntriesFileTest {

    @Test
    public void testAppendEntry() throws IOException{
        RootDir rootdir = new RootDir(new File("logs"));
        FileEntrySequence entrySequence = new FileEntrySequence(rootdir.getLatestGeneration(), 1);
        entrySequence.append(new NoOpEntry(1, 2));
        entrySequence.append(new NoOpEntry(2, 2));
        entrySequence.append(new GeneralEntry(3, 2, "get name".getBytes()));
        entrySequence.commit(3);
        Assert.assertEquals(3, entrySequence.getLastLogIndex());
        entrySequence.close();
    }

    @Test
    public void testRemoveAfter() throws IOException{
        RootDir rootdir = new RootDir(new File("logs"));
        FileEntrySequence entrySequence = new FileEntrySequence(rootdir.getLatestGeneration(), 1);
        Assert.assertEquals(3, entrySequence.getLastLogIndex() );
        entrySequence.removeAfter(0);
        Assert.assertTrue(entrySequence.isEmpty());
        entrySequence.close();
    }
}
