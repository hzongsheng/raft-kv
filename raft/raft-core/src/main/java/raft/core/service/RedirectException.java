package raft.core.service;

import io.netty.channel.ChannelException;
import raft.core.node.NodeId;

public class RedirectException extends ChannelException {

    private final NodeId leaderId;

    public RedirectException(NodeId leaderId) {
        this.leaderId = leaderId;
    }

    public NodeId getLeaderId() {
        return leaderId;

    }
}