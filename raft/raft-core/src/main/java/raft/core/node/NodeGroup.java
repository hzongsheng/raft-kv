package raft.core.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class NodeGroup {
    private final NodeId selfId;
    private Map<NodeId, GroupMember> memberMap;
    private static final Logger logger = LoggerFactory.getLogger(NodeGroup.class);

    NodeGroup(NodeEndpoint endpoint) {
        selfId = endpoint.getId();
        memberMap = Collections.singletonMap(selfId, new GroupMember(endpoint));
    }

    NodeGroup(Collection<NodeEndpoint> endpoints, NodeId selfId) {
        this.memberMap = buildMemberMap(endpoints);
        this.selfId = selfId;
    }

    private Map<NodeId, GroupMember> buildMemberMap(Collection<NodeEndpoint> endpoints) {

        if (endpoints.isEmpty()) {
            throw new IllegalArgumentException("endpoints is empty");
        }

        Map<NodeId, GroupMember> map = new HashMap<NodeId, GroupMember>();
        for (NodeEndpoint endpoint : endpoints) {
            map.put(endpoint.getId(), new GroupMember(endpoint));
        }

        return map;
    }

    GroupMember findSelfMember(){
        return findMember(selfId);
    }

    GroupMember findMember(NodeId id) {
        GroupMember member = getMember(id);
        if (member == null) {
            throw new IllegalArgumentException("No such node " + id);
        }
        return member;
    }

    GroupMember getMember(NodeId id) {
        return memberMap.get(id);
    }

    Collection<GroupMember> listReplicationTarget() {
        HashSet result = new HashSet<GroupMember>(memberMap.values());
        result.remove(memberMap.get(selfId));
        return result;
    }

    Set<NodeEndpoint> listEndpointExceptSelf() {
        Set<NodeEndpoint> endpoints = new HashSet<>();
        for (GroupMember member : memberMap.values()) {
            if (!member.getId().equals(selfId)) {
                endpoints.add(member.getEndpoint());
            }
        }
        return endpoints;
    }

    int getCount() {
        return memberMap.size();
    }

    /**
     * 获得过半节 commitIndex
     * @return true or false
     */
    int getMatchIndexOfMajor() {
        List<NodeMatchIndex> matchIndices = new ArrayList<>();
        for (GroupMember member : memberMap.values()) {
            if (member.isMajor() && !member.idEquals(selfId)) {
                matchIndices.add(new NodeMatchIndex(member.getId(), member.getMatchIndex()));
            }
        }
        int count = matchIndices.size();
        if (count == 0) {
            throw new IllegalStateException("standalone or no major node");
        }
        Collections.sort(matchIndices);
        logger.debug("match indices {}", matchIndices);
        return matchIndices.get(count / 2).getMatchIndex();
    }

    void resetReplicatingStates(int nextLogIndex) {
        for (GroupMember member : memberMap.values()) {
            if (!member.idEquals(selfId)) {
                member.setReplicatingState(new ReplicatingState(nextLogIndex));
            }
        }
    }

    private static class NodeMatchIndex implements Comparable<NodeMatchIndex> {

        private final NodeId nodeId;
        private final int matchIndex;

        NodeMatchIndex(NodeId nodeId, int matchIndex) {
            this.nodeId = nodeId;
            this.matchIndex = matchIndex;
        }

        int getMatchIndex() {
            return matchIndex;
        }

        @Override
        public int compareTo(NodeMatchIndex o) {
            return Integer.compare(this.matchIndex, o.matchIndex);
        }

        @Override
        public String toString() {
            return "<" + nodeId + ", " + matchIndex + ">";
        }

    }
}
