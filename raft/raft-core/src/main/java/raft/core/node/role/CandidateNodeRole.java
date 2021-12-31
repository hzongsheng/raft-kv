package raft.core.node.role;

import raft.core.node.NodeId;
import raft.core.schedule.ElectionTimeout;

public class CandidateNodeRole extends AbstractNodeRole {

    private final int votesCount;
    private final ElectionTimeout electionTimeout;

    public CandidateNodeRole(int term, ElectionTimeout electionTimeout) {
        this(term, 1, electionTimeout);
    }

    public CandidateNodeRole(int term, int votesCount, ElectionTimeout electionTimeout) {
        super(RoleName.CANDIDATE, term);
        this.votesCount = votesCount;
        this.electionTimeout = electionTimeout;
    }

    public int getVotesCount() { return votesCount; }

    @Override
    public void cancelTimeoutOrTask() { electionTimeout.cancel(); }

    @Override
    public NodeId getLeaderId(NodeId selfId) { return null; }

    @Override
    public String toString() {
        return "CandidateNodeRole{" +
                "votesCount=" + votesCount +
                ", electionTimeout=" + electionTimeout +
                '}';
    }
}

