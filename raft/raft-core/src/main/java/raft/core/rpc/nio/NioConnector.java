package raft.core.rpc.nio;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import raft.core.node.NodeEndpoint;
import raft.core.node.NodeId;
import raft.core.rpc.Channel;
import raft.core.rpc.ChannelConnectException;
import raft.core.rpc.Connector;
import raft.core.rpc.message.*;

import java.util.Collection;

public class NioConnector implements Connector {
    private static final Logger logger = LoggerFactory.getLogger(NioConnector.class);

    /**
     * 单 selector
     * 多工作线程 */
    private final NioEventLoopGroup bossNioEventLoopGroup = new NioEventLoopGroup(1);
    private final NioEventLoopGroup workerNioEventLoopGroup;

    /**
     * 上层服务也使用 Netty , 此字段表示是否和上层服务共享 IO 线程池 */
    private final boolean workerGroupShared;
    private final EventBus eventBus;
    private final int port;

    /**
     * 出入口 Channel 组 */
    private final InboundChannelGroup inboundChannelGroup = new InboundChannelGroup();
    private final OutboundChannelGroup outboundChannelGroup;

    public NioConnector(NodeId selfNodeId, EventBus eventBus, int port) {
        this(new NioEventLoopGroup(), false, selfNodeId, eventBus, port);
    }

    public NioConnector(NioEventLoopGroup workerNioEventLoopGroup, NodeId selfNodeId, EventBus eventBus, int port) {
        this(workerNioEventLoopGroup, true, selfNodeId, eventBus, port);
    }

    public NioConnector(NioEventLoopGroup workerNioEventLoopGroup, boolean workerGroupShared, NodeId selfNodeId, EventBus eventBus, int port) {
        this.workerNioEventLoopGroup = workerNioEventLoopGroup;
        this.workerGroupShared = workerGroupShared;
        this.eventBus = eventBus;
        this.port = port;
        outboundChannelGroup = new OutboundChannelGroup(workerNioEventLoopGroup, eventBus, selfNodeId);
    }

    /**
     * 初始化 netty , 加入 Decoder, Encoder, FromRemoteHandler
     */
    @Override
    public void initialize() {
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(workerNioEventLoopGroup, workerNioEventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler( new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new Decoder());
                        pipeline.addLast(new Encoder());
                        pipeline.addLast(new FromRemoteHandler(eventBus, inboundChannelGroup));
                    }
                });
        logger.debug("node listen on port {}", port);
        try {
            serverBootstrap.bind(port).sync();
        } catch (InterruptedException e) {
            throw new ConnectorException("failed to bind port", e);
        }
    }

    @Override
    public void sendRequestVote(RequestVoteRpc rpc, Collection<NodeEndpoint> destinationEndpoints) {
        Preconditions.checkNotNull(rpc);
        Preconditions.checkNotNull(destinationEndpoints);
        for (NodeEndpoint endpoint : destinationEndpoints) {
            logger.debug("send {} to node {}", rpc, endpoint.getId());
            try {
                getChannel(endpoint).writeRequestVoteRpc(rpc);
            } catch (Exception e) {
                logger.warn("failed to send RequetVoteRpc", e);
            }
        }
    }

    @Override
    public void replyRequestVote( RequestVoteResult result,  RequestVoteRpcMessage rpcMessage) {
        Preconditions.checkNotNull(result);
        Preconditions.checkNotNull(rpcMessage);
        logger.debug("reply {} to node {}", result, rpcMessage.getSourceNodeId());
        try {
            rpcMessage.getChannel().writeRequestVoteResult(result);
        } catch (Exception e) {
            logException(e);
        }
    }

    @Override
    public void sendAppendEntries(AppendEntriesRpc rpc, NodeEndpoint destinationEndpoint) {
        Preconditions.checkNotNull(rpc);
        Preconditions.checkNotNull(destinationEndpoint);
        logger.debug("send {} to node {}", rpc, destinationEndpoint.getId());
        try {
            getChannel(destinationEndpoint).writeAppendEntriesRpc(rpc);
        } catch (Exception e) {
            logException(e);
        }
    }

    @Override
    public void replyAppendEntries(AppendEntriesResult result, AppendEntriesRpcMessage rpcMessage) {
        Preconditions.checkNotNull(result);
        Preconditions.checkNotNull(rpcMessage);
        logger.debug("reply {} to node {}", result, rpcMessage.getSourceNodeId());
        try {
            rpcMessage.getChannel().writeAppendEntriesResult(result);
        } catch (Exception e) {
            logException(e);
        }
    }

    @Override
    public void resetChannels() {
        inboundChannelGroup.closeAll();
    }

    @Override
    public void close() {
        logger.debug("close connector");
        inboundChannelGroup.closeAll();
        outboundChannelGroup.closeAll();
        bossNioEventLoopGroup.shutdownGracefully();
        if (!workerGroupShared) {
            workerNioEventLoopGroup.shutdownGracefully();
        }
    }

    private void logException(Exception e) {
        if (e instanceof ChannelConnectException) {
            logger.warn(e.getMessage());
        } else {
            logger.warn("failed to process channel", e);
        }
    }

    private Channel getChannel(NodeEndpoint endpoint) {
        return outboundChannelGroup.getOrConnect(endpoint.getId(), endpoint.getAddress());
    }
}








































