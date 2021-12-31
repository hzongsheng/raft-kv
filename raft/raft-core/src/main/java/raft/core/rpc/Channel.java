package raft.core.rpc;

import raft.core.rpc.message.*;

public interface Channel {

    void writeRequestVoteRpc( RequestVoteRpc rpc);

    void writeRequestVoteResult( RequestVoteResult result);

    void writeAppendEntriesRpc( AppendEntriesRpc rpc);

    void writeAppendEntriesResult( AppendEntriesResult result);

    void close();
}
