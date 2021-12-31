package raft.kvstore.message;

import raft.core.node.NodeId;

public class Redirect {
    private final String leaderId;

    public Redirect(String leaderId) {
        this.leaderId = leaderId;
    }

    public Redirect(NodeId leaderId) {
        this(leaderId == null ? null : leaderId.getValue());
    }

    public String getLeaderId() {
        return leaderId;
    }

    @Override
    public String toString() {
        return "Redirect{" +
                "leaderId='" + leaderId + '\'' +
                '}';
    }
}
