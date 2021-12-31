package raft.core.rpc.nio;

import com.google.common.eventbus.EventBus;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.core.node.NodeId;
/**
 *  在通道初始化的时候发送 NodeId 消息, 以建立对端节点和 Channel 的关联
 */
public class ToRemoteHandler extends AbstractHandler{
    private static final Logger logger = LoggerFactory.getLogger(ToRemoteHandler.class);

    private final NodeId selfNodeId;

    public ToRemoteHandler(EventBus eventBus, NodeId remoteId, NodeId selfNodeId) {
        super(eventBus);
        this.remoteId = remoteId;
        this.selfNodeId= selfNodeId;
    }

    /** 通道初始化的时候就向对方节点发送 Node 消息, 以建立对方 NodeId 和 Channel 之间的联系
     *
     * @param ctx handler context
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx){
        ctx.write(selfNodeId);
        channel = new NioChannel(ctx.channel());
    }
}
