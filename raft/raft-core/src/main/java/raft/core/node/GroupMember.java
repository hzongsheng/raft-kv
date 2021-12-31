package raft.core.node;

/**
 * 每个节点维护的全局成员信息, 包含了节点的 endpoint , replicatingState , major 信息
 */
public class GroupMember{
    private final NodeEndpoint endpoint;
    private ReplicatingState replicatingState;
    private boolean major;

    public GroupMember(NodeEndpoint endpoint) {
        this(endpoint, null, true);
    }

    public GroupMember(NodeEndpoint endpoint, ReplicatingState replicatingState, boolean major) {
        this.endpoint = endpoint;
        this.replicatingState = replicatingState;
        this.major = major;
    }


    private ReplicatingState ensureReplicatingState(){
        if(replicatingState == null){
            throw new IllegalArgumentException("replication state not set");
        }
        return replicatingState;
    }

    public boolean idEquals(NodeId selfId) {
        return selfId.equals(endpoint.getId());
    }

    public NodeEndpoint getEndpoint() {
        return endpoint;
    }

    public NodeId getId(){
        return endpoint.getId();
    }

    void setReplicatingState(ReplicatingState replicatingState) {
        this.replicatingState = replicatingState;
    }

    int getNextIndex() { return ensureReplicatingState().getNextIndex(); }

    boolean advanceReplicatingState(int lastEntryIndex) {
        return ensureReplicatingState().advance(lastEntryIndex);
    }

    boolean backOffNextIndex() {
        return ensureReplicatingState().backOffNextIndex();
    }

    boolean isMajor(){
        return major;
    }

    int getMatchIndex() {
        return ensureReplicatingState().getMatchIndex();
    }

}
