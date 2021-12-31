package raft.core.node;

import org.junit.Assert;
import raft.core.node.role.CandidateNodeRole;
import raft.core.node.role.FollowerNodeRole;
import raft.core.node.role.LeaderNodeRole;
import raft.core.rpc.MockConnector;
import raft.core.rpc.message.*;
import raft.core.rpc.nio.NioChannel;
import raft.core.schedule.NullScheduler;
import raft.core.support.DirectTaskExecutor;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

public class NodeImplTest {
    private NodeBuilder newNodeBuilder(NodeId selfId, NodeEndpoint... endpoints){
        return new NodeBuilder(Arrays.asList(endpoints), selfId)
                .setScheduler(new NullScheduler())
                .setConnector(new MockConnector())
                .setTaskExecutor(new DirectTaskExecutor());
    }

    @Test
    public void testStart(){
        NodeImpl node = (NodeImpl) newNodeBuilder(NodeId.of("A"), new NodeEndpoint("A", "localhost", 2333)).build();
        node.start();
        FollowerNodeRole role = (FollowerNodeRole) node.getRole();
        Assert.assertEquals(0, role.getTerm());
        Assert.assertNull(role.getVotedFor());
    }

    @Test
    public void testElectionTimeoutWhenFollower(){
        NodeImpl node = (NodeImpl) newNodeBuilder(
                NodeId.of("A"),
                new NodeEndpoint("A", "localhost", 2333),
                new NodeEndpoint("B", "localhost", 2334),
                new NodeEndpoint("C", "localhost", 2335)
        ).build();
        node.start();
        node.electionTimeout();
        CandidateNodeRole role = (CandidateNodeRole) node.getRole();
        Assert.assertEquals(1, role.getTerm());
        Assert.assertEquals(1, role.getVotesCount());

        MockConnector mockConnector = (MockConnector) node.getContext().getConnector();
        RequestVoteRpc rpc = (RequestVoteRpc) mockConnector.getRpc();

        Assert.assertEquals(1, rpc.getTerm());
        Assert.assertEquals(NodeId.of("A"), rpc.getCandidateId());
        Assert.assertEquals(0, rpc.getLastLogIndex());
        Assert.assertEquals(0, rpc.getLastLogTerm());
    }

    /**
     * 测试当 C 请求投票时,
     * 节点 node term 为 1, 选择投票, 投的对象是 C
     */
    @Test
    public void testOnReceiveRequestRpcFollower(){
        NodeImpl node = (NodeImpl) newNodeBuilder(
                NodeId.of("A"),
                new NodeEndpoint("A", "localhost", 2333),
                new NodeEndpoint("B", "localhost", 2334),
                new NodeEndpoint("C", "localhost", 2335)
        ).build();
        NodeId NodeIdC = NodeId.of("C");

        node.start();
        RequestVoteRpc rpc = new RequestVoteRpc();
        rpc.setTerm(1);
        rpc.setCandidateId(NodeIdC);
        rpc.setLastLogIndex(0);
        rpc.setLastLogTerm(0);
//        node.onReceiveRequestVoteRpc(new RequestVoteRpcMessage(rpc, NodeIdC));
        MockConnector mockConnector = (MockConnector) node.getContext().getConnector();
        RequestVoteResult result = (RequestVoteResult) mockConnector.getResult();
        Assert.assertEquals(1, result.getTerm());
        Assert.assertTrue(result.isVoteGranted());
        Assert.assertEquals(NodeIdC, ((FollowerNodeRole) node.getRole()).getVotedFor());
    }

    /**
     * 测试节点 node 收到了一个 term 为 1, voteGranted 为 true 的投票结果之后成为 leader
     */
    @Test
    public void testOnReceiveRequestVoteResult(){
        NodeImpl node = (NodeImpl) newNodeBuilder(
                NodeId.of("A"),
                new NodeEndpoint("A", "localhost", 2333),
                new NodeEndpoint("B", "localhost", 2334),
                new NodeEndpoint("C", "localhost", 2335)
        ).build();

        node.start();
        node.electionTimeout();
        node.onReceiveRequestVoteResult(new RequestVoteResult(1, true));
        LeaderNodeRole role = (LeaderNodeRole) node.getRole();
        Assert.assertEquals(1, role.getTerm());
    }

    @Test
    public void testReplicateLog(){
        NodeImpl node = (NodeImpl) newNodeBuilder(
                NodeId.of("A"),
                new NodeEndpoint("A", "localhost", 2333),
                new NodeEndpoint("B", "localhost", 2334),
                new NodeEndpoint("C", "localhost", 2335)
        ).build();
        node.start();
        MockConnector mockConnector = (MockConnector) node.getContext().getConnector();
        node.electionTimeout();

        Assert.assertEquals(1, mockConnector.getMessageCount());

        node.onReceiveRequestVoteResult(new RequestVoteResult(1, true));

        // 发送两条日志消息, 给 B 和 C
        // 加上之前的选举请求一共四条
        node.replicateLog();

        Assert.assertEquals(3, mockConnector.getMessageCount());
        List<MockConnector.Message> messages = mockConnector.getMessages();
        Set<NodeId> destinationNodeIds = messages.subList(1, 3).stream()
                .map(MockConnector.Message::getDestinationNodeId)
                .collect(Collectors.toSet());
        Assert.assertEquals(2, destinationNodeIds.size());
        Assert.assertTrue(destinationNodeIds.contains(NodeId.of("B")));
        Assert.assertTrue(destinationNodeIds.contains(NodeId.of("C")));
        AppendEntriesRpc rpc = (AppendEntriesRpc) messages.get(2).getRpc();
        Assert.assertEquals(1, rpc.getTerm());
    }

    /**
     * 测试收到 leader 节点的 appendEntriesRpc 消息之后的动作
     * 节点的 term 变为
     */
    @Test
    public void testOnReceiveAppendEntriesRpcFollower() {
        NodeImpl node = (NodeImpl) newNodeBuilder(
                NodeId.of("A"),
                new NodeEndpoint("A", "localhost", 2333),
                new NodeEndpoint("B", "localhost", 2334),
                new NodeEndpoint("C", "localhost", 2335)
        ).build();
        node.start();

        Assert.assertEquals(node.getRole().getTerm(), 0);

        AppendEntriesRpc rpc = new AppendEntriesRpc();
        rpc.setTerm(2);
        rpc.setLeaderId(NodeId.of("B"));
//        node.onReceiveAppendEntriesRpc(new AppendEntriesRpcMessage(rpc, NodeId.of("B")));
        MockConnector mockConnector = (MockConnector) node.getContext().getConnector();
        AppendEntriesResult result = (AppendEntriesResult) mockConnector.getResult();
        Assert.assertEquals(result.getTerm(), 2);
        Assert.assertTrue(result.isSuccess());
        FollowerNodeRole role = (FollowerNodeRole) node.getRole();
        Assert.assertEquals(role.getTerm(), 2);
        Assert.assertEquals(role.getLeaderId(), NodeId.of("B"));
    }
}

