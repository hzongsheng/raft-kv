package raft.core.rpc.message;

public class RequestVoteResult {
    /**
     * 选举 term
     * 是否投票给候选者
     */
    private final int term;
    private final boolean voteGranted;

    public RequestVoteResult(int term, boolean voteGranted) {
        this.term = term;
        this.voteGranted = voteGranted;
    }

    public boolean isVoteGranted() { return voteGranted; }

    public int getTerm(){ return term; }

    @Override
    public String toString() {
        return "RequestVoteResult{" +
                "term=" + term +
                ", voteGranted=" + voteGranted +
                '}';
    }
}
