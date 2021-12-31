package raft.core.node.store;

import raft.core.node.NodeId;

public interface NodeStore {
    int getTerm();

    void setTerm(int term);

    NodeId getVotedFor();

    void setVotedFor(NodeId votedFor);

    void close();
}
