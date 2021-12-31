package raft.core.rpc.nio;

import io.netty.channel.ChannelException;
import raft.core.rpc.Channel;
import raft.core.rpc.message.*;

/**
 * netty channel 的包装类, 直接使用 netty 实现四类消息的发送
 */
public class NioChannel implements Channel {

    private final io.netty.channel.Channel nettyChannel;

    NioChannel(io.netty.channel.Channel nettyChannel) {
        this.nettyChannel = nettyChannel;
    }

    @Override
    public void writeRequestVoteRpc(RequestVoteRpc rpc ) {
        nettyChannel.writeAndFlush(rpc);
    }

    @Override
    public void writeRequestVoteResult(RequestVoteResult result ) {
        nettyChannel.writeAndFlush(result);
    }

    @Override
    public void writeAppendEntriesRpc(AppendEntriesRpc rpc ) {
        nettyChannel.writeAndFlush(rpc);
    }

    @Override
    public void writeAppendEntriesResult(AppendEntriesResult result ) {
        nettyChannel.writeAndFlush(result);
    }

    @Override
    public void close() {
        try {
            nettyChannel.close().sync();
        } catch (InterruptedException e) {
            throw new ChannelException("failed to close", e);
        }
    }

    io.netty.channel.Channel getDelegate() {
        return nettyChannel;
    }

}
