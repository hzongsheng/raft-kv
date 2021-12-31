package raft.core.node;

import com.google.common.eventbus.EventBus;
import raft.core.log.Log;
import raft.core.node.config.NodeConfig;
import raft.core.node.store.NodeStore;
import raft.core.rpc.Connector;
import raft.core.schedule.Scheduler;
import raft.core.support.TaskExecutor;

public class NodeContext {
    /**
     * selfId 当前节点 ID
     * group 节点成员列表
     * connector RPC 组件
     * scheduler 定时器组件
     * eventBus
     * taskExecutor 主线程执行器
     * store 部分角色状态数据存储
     */
    private NodeId selfId;
    private NodeGroup group;
    private Connector connector;
    private Log log;

    private Scheduler scheduler;
    private EventBus eventBus;
    private TaskExecutor taskExecutor;
    private NodeStore store;
    private NodeConfig config;


    public NodeStore getStore() {
        return store;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public Connector getConnector() {
        return connector;
    }

    public TaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public NodeGroup getGroup() {
        return group;
    }

    public Log getLog() { return log; }

    public NodeConfig getConfig() {
        return config;
    }

    public NodeId selfId() { return selfId; }

    public void setConfig(NodeConfig config) {
        this.config = config;
    }

    public void setGroup(NodeGroup group) {
        this.group = group;
    }

    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void setStore(NodeStore store) {
        this.store = store;
    }

    public void setSelfId(NodeId selfId){
        this.selfId = selfId;
    }

    public void setLog(Log log) { this.log = log; }

    GroupMember findMember(NodeId sourceNodeId){
        return group.getMember(sourceNodeId);
    }
}
