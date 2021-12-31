package raft.kvstore.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import raft.kvstore.message.CommandRequest;
import raft.kvstore.message.GetCommand;
import raft.kvstore.message.SetCommand;

public class ServiceHandler extends ChannelInboundHandlerAdapter {

    private final Service service;

    public ServiceHandler(Service service) {
        this.service = service;
    }

    /**
     * 持有一个 service
     *
     * 接受到 GetCommand 或者 SetCommand 时, 将此消息和 channel 封装到 CommandRequest 中, 并调用 service 执行命令
     *
     * @param ctx ctx
     * @param msg msg
     * @throws Exception exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof GetCommand){
            service.get(new CommandRequest<>
                    ((GetCommand) msg, ctx.channel()));
        }else if(msg instanceof SetCommand){
            service.set(new CommandRequest<>
                    ((SetCommand) msg, ctx.channel()));
        }
    }
}
