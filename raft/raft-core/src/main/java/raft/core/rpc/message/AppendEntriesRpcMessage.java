package raft.core.rpc.message;

import raft.core.node.NodeId;
import raft.core.rpc.Channel;

public class AppendEntriesRpcMessage extends AbstractRpcMessage{

    public AppendEntriesRpcMessage(Object rpc, NodeId sourceNodeId, Channel channel) {
        super(rpc, sourceNodeId, channel);
    }
}
