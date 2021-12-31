package raft.core.rpc;

import io.netty.channel.ChannelException;

public class ChannelConnectException extends ChannelException {

    public ChannelConnectException(Throwable cause) {
        super(cause);
    }

    public ChannelConnectException(String message, Throwable cause) {
        super(message, cause);
    }
}
