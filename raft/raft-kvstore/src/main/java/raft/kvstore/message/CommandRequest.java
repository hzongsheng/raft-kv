package raft.kvstore.message;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

/**
 * Command 和 client 的 Channel 的组合
 *
 * @param <T>
 */
public class CommandRequest<T> {
    private final T command;
    private final Channel channel;

    public CommandRequest(T command, Channel channel) {
        this.command = command;
        this.channel = channel;
    }

    public void reply(Object response){
        this.channel.writeAndFlush(response);
    }

    public void addCloseListener(Runnable runnable){
        this.channel.closeFuture().addListener((ChannelFutureListener)future -> runnable.run());
    }

    public T getCommand() { return command; }

}