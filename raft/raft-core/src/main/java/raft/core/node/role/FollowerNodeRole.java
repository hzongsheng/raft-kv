package raft.core.node.role;

import raft.core.node.NodeId;
import raft.core.schedule.ElectionTimeout;

public class FollowerNodeRole extends AbstractNodeRole {
    private  final NodeId votedFor;
    private final NodeId leaderId;
    private final ElectionTimeout electionTimeout;

    public FollowerNodeRole(int term, NodeId votedFor, NodeId leaderId, ElectionTimeout electionTimeout){
        super(RoleName.FOLLOWER, term);
        this.votedFor = votedFor;
        this.leaderId = leaderId;
        this.electionTimeout = electionTimeout;
    }

    public NodeId getLeaderId() { return leaderId; }

    @Override
    public NodeId getLeaderId(NodeId selfId) { return leaderId; }

    public NodeId getVotedFor() { return votedFor; }


    @Override
    public void cancelTimeoutOrTask(){ electionTimeout.cancel(); }

    @Override
    public String toString(){
        return "FollowerNodeRole{" +
                "term = " + term +
                ", leaderId = " + leaderId +
                ", votedFor = " + votedFor +
                ", electionTimeout=" + electionTimeout +
                '}';
    }
}
