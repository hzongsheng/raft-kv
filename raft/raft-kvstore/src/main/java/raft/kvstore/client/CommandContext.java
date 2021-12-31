package raft.kvstore.client;

import raft.core.node.NodeId;
import raft.core.rpc.Address;
import raft.core.service.ServerRouter;

import java.util.Map;

public class CommandContext {
    /** 维护的全局 server map */
    private final Map<NodeId, Address> serverMap;
    /** 是否在运行的标志 , Exit 命令可关闭 */
    private boolean running = false;
    /** 持有一个 Client */
    private Client client;

    public CommandContext(Map<NodeId, Address> serverMap) {
        this.serverMap = serverMap;
        this.client = new Client(buildServerRouter(serverMap));
    }

    private ServerRouter buildServerRouter(Map<NodeId, Address> serverMap) {
        ServerRouter router = new ServerRouter();
        for (NodeId nodeId : serverMap.keySet()) {
            Address address = serverMap.get(nodeId);
            router.add(nodeId, new SocketChannel(address.getHost(), address.getPort()));
        }
        return router;
    }

    Client getClient() { return client; }

    NodeId getClientLeader() { return client.getServerRouter().getLeaderId(); }

    void setClientLeader(NodeId nodeId) { client.getServerRouter().setLeaderId(nodeId); }

    void setRunning(boolean running) { this.running = running; }

    boolean isRunning() { return running; }

    void printSeverList() {
        for (NodeId nodeId : serverMap.keySet()) {
            Address address = serverMap.get(nodeId);
            System.out.println(nodeId + "- " + address.getHost() + ":" + address.getPort());
        }
    }

}
