package raft.core.rpc.nio;

public class ConnectorException extends RuntimeException{
    public ConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
