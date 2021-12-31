package raft.kvstore.client;

import raft.core.service.ServerRouter;
import raft.kvstore.Protos;
import raft.kvstore.message.GetCommand;
import raft.kvstore.message.SetCommand;

public class Client {
    public static final String VERSION = "1.0";

    private final ServerRouter serverRouter;

    public Client(ServerRouter serverRouter) {
        this.serverRouter = serverRouter;
    }

    public byte[] get(String key) {
        return (byte[]) serverRouter.send(new GetCommand(key));
    }

    public byte[] set(String key, byte[] value){
        return (byte[]) serverRouter.send(new SetCommand(key, value));
    }

    public ServerRouter getServerRouter() {
        return serverRouter;
    }

}
