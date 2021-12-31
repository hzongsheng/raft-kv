package raft.core.rpc.nio;

import com.google.common.eventbus.EventBus;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.core.node.NodeId;

/**
 * 在收到对端的 NodeId 请求时, 创建这个节点的 入口连接, 并将其加入到 InboundChannelGroup 中
 */
public class FromRemoteHandler extends AbstractHandler{

    private static final Logger logger = LoggerFactory.getLogger(FromRemoteHandler.class);

    private final InboundChannelGroup channelGroup;

    FromRemoteHandler(EventBus eventBus, InboundChannelGroup channelGroup) {
        super(eventBus);
        this.channelGroup = channelGroup;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
        if (msg instanceof NodeId) {
            remoteId = (NodeId) msg;
            NioChannel nioChannel = new NioChannel(ctx.channel());
            channel = nioChannel;
            channelGroup.add(remoteId, nioChannel);
            return;
        }

        logger.debug("receive {} from {}", msg, remoteId);
    }

}
