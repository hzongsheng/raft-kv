package raft.core.rpc;

import raft.core.node.NodeEndpoint;
import raft.core.rpc.message.*;

import java.util.Collection;

public interface Connector {
    void initialize();

    void sendRequestVote(RequestVoteRpc rpc, Collection<NodeEndpoint> destinationEndpoints);

    void replyRequestVote(RequestVoteResult result, RequestVoteRpcMessage rpcMessage);

    void sendAppendEntries(AppendEntriesRpc rpc, NodeEndpoint destinationEndpoint);

    void replyAppendEntries(AppendEntriesResult result, AppendEntriesRpcMessage rpcMessage);

    void resetChannels();

    void close();
}
