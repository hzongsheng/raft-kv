package raft.core.rpc;

import raft.core.node.NodeEndpoint;
import raft.core.node.NodeId;
import raft.core.rpc.message.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class MockConnector implements Connector{


    private LinkedList<Message> messages = new LinkedList<>();

    @Override
    public void initialize(){}

    @Override
    public void sendRequestVote(RequestVoteRpc rpc, Collection<NodeEndpoint> destinationEndpoints) {
        Message m = new Message();
        m.rpc = rpc;
        messages.add(m);
    }

    @Override
    public void replyRequestVote(RequestVoteResult result, RequestVoteRpcMessage rpcMessage) {
        Message m = new Message();
        m.result = result;
        m.destinationNodeId = rpcMessage.getSourceNodeId();
        messages.add(m);
    }

    @Override
    public void sendAppendEntries(AppendEntriesRpc rpc, NodeEndpoint destinationEndpoint) {
        Message m = new Message();
        m.rpc = rpc;
        m.destinationNodeId = destinationEndpoint.getId();
        messages.add(m);
    }

    @Override
    public void replyAppendEntries(AppendEntriesResult result, AppendEntriesRpcMessage rpcMessage) {
        Message m = new Message();
        m.result = result;
        m.destinationNodeId = rpcMessage.getSourceNodeId();
        messages.add(m);
    }

    @Override
    public void resetChannels() {
    }

    @Override
    public void close() { }

    /** 获取最后一条消息
     */
    public Message getLastMessage() {
        return messages.isEmpty() ? null : messages.getLast();
    }

    /** 获取最后一条消息或者空消息
     */
    private Message getLastMessageOrDefault() {
        return messages.isEmpty() ? new Message() : messages.getLast();
    }

    /** 获取最后一条 RPC 消息
     */
    public Object getRpc() {
        return getLastMessageOrDefault().rpc;
    }

    /** 获取最后一条 Result 消息
     */
    public Object getResult() {
        return getLastMessageOrDefault().result;
    }

    /** 获取最后一条消息的目标节点
     */
    public NodeId getDestinationNodeId() {
        return getLastMessageOrDefault().destinationNodeId;
    }

    /** 获取消息的总数量
      */
    public int getMessageCount() {
        return messages.size();
    }

    /** 获取所有的消息
     */
    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    // 清除消息
    public void clearMessage() {
        messages.clear();
    }

    public static class Message{
        private Object rpc;
        private NodeId destinationNodeId;
        private  Object result;

        public Object getRpc() { return rpc; }

        public NodeId getDestinationNodeId() { return destinationNodeId; }

        public Object getResult() { return result; }

        @Override
        public String toString() {
            return "Message{" +
                    "rpc=" + rpc +
                    ", destinationNodeId=" + destinationNodeId +
                    ", result=" + result +
                    '}';
        }
    }
}
