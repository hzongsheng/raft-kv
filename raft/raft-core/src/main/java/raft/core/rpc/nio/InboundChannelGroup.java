package raft.core.rpc.nio;

import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.core.node.NodeId;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 这个类用来记录所有的入口连接, 但是并不需要进行按 NodeId 查找 Channel 的操作,
 * 只需在节点成为 leader 之后, 关闭所有入口连接
 */
public class InboundChannelGroup {
    private static final Logger logger = LoggerFactory.getLogger(InboundChannelGroup.class);
    /** 需保证线程安全, 但又不需要查找操作 */
    private final List<NioChannel> channels = new CopyOnWriteArrayList<>();

    public void add(NodeId remoteId, NioChannel channel) {
        logger.debug("channel INBOUND-{} connected", remoteId);
        channel.getDelegate().closeFuture().addListener((ChannelFutureListener) future -> {
            logger.debug("channel INBOUND-{} disconnected", remoteId);
            remove(channel);
        });
    }

    private void remove(NioChannel channel) {
        channels.remove(channel);
    }

    void closeAll() {
        logger.debug("close all inbound channels");
        for (NioChannel channel : channels) {
            channel.close();
        }
    }
}
