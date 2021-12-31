package raft.core.node;

import com.google.common.eventbus.EventBus;
import io.netty.channel.nio.NioEventLoopGroup;
import raft.core.log.FileLog;
import raft.core.log.Log;
import raft.core.log.MemoryLog;
import raft.core.log.RootDir;
import raft.core.node.config.NodeConfig;
import raft.core.node.store.FileNodeStore;
import raft.core.node.store.MemoryNodeStore;
import raft.core.node.store.NodeStore;
import raft.core.rpc.Connector;
import raft.core.rpc.nio.NioConnector;
import raft.core.schedule.DefaultScheduler;
import raft.core.schedule.Scheduler;
import raft.core.support.SingleThreadTaskExecutor;
import raft.core.support.TaskExecutor;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

public class NodeBuilder {
    private final NodeGroup group;
    private final NodeId selfId;
    private final EventBus eventBus;

    private NodeConfig config = new NodeConfig();
    private NodeStore store = null;
    private Scheduler scheduler = null;
    private Connector connector = null;
    private TaskExecutor taskExecutor = null;
    private Log log = null;
    private boolean standby = false;
    private NioEventLoopGroup workerNioEventLoopGroup = null;

    public NodeBuilder(Collection<NodeEndpoint> endpoints, NodeId selfId) {
        this.group = new NodeGroup(endpoints, selfId);
        this.selfId = selfId;
        // 每个节点新建一个 EventBus
        this.eventBus = new EventBus(selfId.getValue());
    }

    public NodeBuilder( NodeEndpoint endpoint) {
        this(Collections.singletonList(endpoint), endpoint.getId());
    }

    public NodeBuilder setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        return this;
    }

    public NodeBuilder setConnector(Connector connector) {
        this.connector = connector;
        return this;
    }

    public NodeBuilder setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        return this;
    }

    public NodeBuilder setStandby(boolean standby) {
        this.standby = standby;
        return this;
    }

    public Node build() {
        return new NodeImpl(buildContext());
    }

    private NodeContext buildContext() {
        NodeContext context = new NodeContext();
        context.setGroup(group);
        context.setSelfId(selfId);
        context.setEventBus(eventBus);
        context.setConfig(new NodeConfig());
        context.setStore(store != null ? store : new MemoryNodeStore());
        context.setScheduler(scheduler != null ? scheduler : new DefaultScheduler(config.getMinElectionTimeout(),config.getMaxElectionTimeout(),config.getLogReplicationDelay(),config.getLogReplicationInterval()));
        context.setConnector(connector != null ? connector : createNioConnector());
        context.setTaskExecutor(taskExecutor != null ? taskExecutor : new SingleThreadTaskExecutor("node"));
        context.setLog(log != null ? log : new MemoryLog(eventBus));
        return context;
    }

    public NodeBuilder setDataDir( String dataDirPath) {
        if (dataDirPath == null || dataDirPath.isEmpty()) {
            return this;
        }
        File dataDir = new File(dataDirPath);
        if (!dataDir.isDirectory() || !dataDir.exists()) {
            throw new IllegalArgumentException("[" + dataDirPath + "] not a directory, or not exists");
        }
        log = new FileLog(dataDir, eventBus);
        store = new FileNodeStore(new File(dataDir, FileNodeStore.FILE_NAME));
        return this;
    }

    private NioConnector createNioConnector() {
        int port = group.findSelfMember().getEndpoint().getPort();
        if (workerNioEventLoopGroup != null) {
            return new NioConnector(workerNioEventLoopGroup, selfId, eventBus, port);
        }
        return new NioConnector(new NioEventLoopGroup(config.getNioWorkerThreads()), false, selfId, eventBus, port);
    }
}
