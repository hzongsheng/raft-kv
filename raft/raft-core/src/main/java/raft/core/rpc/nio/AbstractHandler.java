package raft.core.rpc.nio;

import com.google.common.eventbus.EventBus;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.core.node.NodeId;
import raft.core.rpc.Channel;
import raft.core.rpc.message.*;

import java.util.Objects;

/** 建立 对方节点-Channel, 核心组件-通信组件 的关联
 *  在收到 AppendEntriesResult, RequestVoteRpcResult 消息时将其封装为 AppendEntriesRpcMessage , RequestVoteRpcMessage 消息
 *  维护同步日志时多个消息的顺序
 *
 *  子类实现 ToRemoteHandler 发送 NodeId 的初始化任务,
 *  FromRemoteHandler 完成接受 NodeId 创建入口连接并加入到 InboundChannelGroup 中维护
 */
public abstract class AbstractHandler extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(AbstractHandler.class);

    /** 用来将 Channel 和核心组件联系起来 */
    protected final EventBus eventBus;

    /** 用来将 Channel 和对方的 NodeId 联系起来*/
    NodeId remoteId;
    protected Channel channel;

    /** 上次发送的 AppendEntriesRpc 请求, 以维护多个同步日志的顺序 */
    private AppendEntriesRpc lastAppendEntriesRpc;

    /**
     * 使用 EventBus 解决 rpc 组件和 core 组件之间的双向依赖
     *
     * @param eventBus eventBus
     */
    public AbstractHandler(EventBus eventBus) { this.eventBus = eventBus; }

    /** 在 Decoder 之后运行 */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        // 请求投票
        if (msg instanceof RequestVoteRpc) {
            RequestVoteRpc rpc = (RequestVoteRpc) msg;
            eventBus.post(new RequestVoteRpcMessage(rpc, remoteId, channel));
        }
        // 投票回复
        else if (msg instanceof RequestVoteResult) {
            eventBus.post(msg);
        }
        // leader 同步日志的请求
        else if (msg instanceof AppendEntriesRpc) {
            AppendEntriesRpc rpc = (AppendEntriesRpc) msg;
            eventBus.post(new AppendEntriesRpcMessage(rpc, remoteId, channel));
        }
        // 同步日志的回复
        else if (msg instanceof AppendEntriesResult) {
            AppendEntriesResult result = (AppendEntriesResult) msg;
            if (lastAppendEntriesRpc == null) {
                logger.warn("no last append entries rpc");
            } else {
                if (!Objects.equals(result.getRpcMessageId(), lastAppendEntriesRpc.getMessageId())) {
                    logger.warn("incorrect append entries rpc message id {}, expected {}", result.getRpcMessageId(), lastAppendEntriesRpc.getMessageId());
                } else {
                    eventBus.post(new AppendEntriesResultMessage(result, remoteId, lastAppendEntriesRpc));
                    lastAppendEntriesRpc = null;
                }
            }
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof AppendEntriesRpc) {
            lastAppendEntriesRpc = (AppendEntriesRpc) msg;
        }
        super.write(ctx, msg, promise);
    }
}
