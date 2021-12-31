package raft.core.node;

import com.google.common.eventbus.Subscribe;
import java.util.Objects;

import com.google.common.util.concurrent.FutureCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import raft.core.log.entry.EntryMeta;
import raft.core.log.statemachine.StateMachine;
import raft.core.node.role.*;
import raft.core.schedule.*;
import raft.core.rpc.message.*;
import raft.core.node.store.NodeStore;


public class NodeImpl implements Node{
    private final NodeContext context;
    private AbstractNodeRole role;
    private boolean started;
    private static final Logger logger = LoggerFactory.getLogger(NodeImpl.class);

    // 异步任务回调
    private static final FutureCallback<Object> LOGGING_FUTURE_CALLBACK = new FutureCallback<Object>() {
        @Override
        public void onSuccess(Object result) {
        }

        @Override
        public void onFailure( Throwable t) {
            logger.warn("failure", t);
        }
    };

    NodeImpl(NodeContext context){
        this.context = context;
    }

    public NodeContext getContext() { return context; }

    public AbstractNodeRole getRole() { return role; }


    @Override
    public void registerStateMachine(StateMachine stateMachine) {
        context.getLog().setStateMachine(stateMachine);
    }

    @Override
    public RoleNameAndLeaderId getRoleNameAndLeaderId() {
        switch (role.getName()){
            case FOLLOWER:
                return new RoleNameAndLeaderId(RoleName.FOLLOWER, role.getLeaderId(context.selfId()));

            case CANDIDATE:
                return new RoleNameAndLeaderId(RoleName.CANDIDATE, role.getLeaderId(context.selfId()));

            case LEADER:
                return new RoleNameAndLeaderId(RoleName.LEADER, context.selfId());

            default:
                throw new IllegalStateException("unexpected role name" + role.getName());
        }
    }

    @Override
    public void appendLog(byte[] commandBytes) {
        ensureLeader();
        context.getTaskExecutor().submit(() -> {
            // 本地 appendLog
            context.getLog().appendEntry(role.getTerm(), commandBytes);
            // rpc 让 follower 节点 appendLog
            doReplicateLog();
        }, LOGGING_FUTURE_CALLBACK);
    }

    private void ensureLeader() {
        RoleNameAndLeaderId result = role.getNameAndLeaderId(context.selfId());
        if (result.getRoleName() == RoleName.LEADER) {
            return;
        }
        NodeEndpoint endpoint = result.getLeaderId() != null ? context.getGroup().findMember(result.getLeaderId()).getEndpoint() : null;
        throw new NotLeaderException(result.getRoleName(), endpoint);
    }
    /**
     * 节点启动 完成注册到 context 的 eventBus , 初始化 Connector
     * changeToRole(Follower) 转化为 Follower 角色, 其中 Follower 角色的 Term 和 votedFor 要从 store 中读取, 防止重启前和重启后投给不同的节点
     */
    @Override
    public synchronized void start() {
        if(started) {
            return;
        }
        // 将此 node 注册到 EventBus 中
        context.getEventBus().register(this);

        // 初始化 Channel 等
        context.getConnector().initialize();
        // 重启后从 store 中读取
        NodeStore store = context.getStore();
        changeToRole(new FollowerNodeRole(
                store.getTerm(), store.getVotedFor(), null, scheduleElectionTimeout()
        ));
        started = true;
    }

    /**
     * 此处的 submit 默认是在定时器的线程中 , 为了在主线程中执行, 需要下面的 electionTimeOut 在主线程中提交实际的任务
     * 即这个方法里只是提交了一个 提交任务到主线程 的任务
     */
    private ElectionTimeout scheduleElectionTimeout(){
        return context.getScheduler().scheduleElectionTimeout(this::electionTimeout);
    }

    /**
     * 上面的 ElectionTimeOut 只是 submit 这个  "submit 任务到主线程的任务"
     */
    void electionTimeout(){
        context.getTaskExecutor().submit(this::doProcessElectionTimeout, LOGGING_FUTURE_CALLBACK);
    }

    private void doProcessElectionTimeout(){
        if(role.getName() == RoleName.LEADER){
            logger.warn("node {}, current role is leader ignore election timeout" , context.selfId());
            return;
        }

        int newTerm = role.getTerm() + 1;
        role.cancelTimeoutOrTask();
        logger.debug("node {} starting election", context.selfId());
        // 只有改变 NodeRole 时会涉及 Term 改变, 只需在 changeToRole 里持久化 store
        changeToRole(new CandidateNodeRole(newTerm, scheduleElectionTimeout()));

        RequestVoteRpc rpc = new RequestVoteRpc();
        EntryMeta lastEntryMeta = context.getLog().getLastEntryMeta();
        rpc.setTerm(newTerm);
        rpc.setCandidateId(context.selfId());
        rpc.setLastLogIndex(lastEntryMeta.getIndex());
        rpc.setLastLogTerm(lastEntryMeta.getTerm());
        context.getConnector().sendRequestVote(rpc, context.getGroup().listEndpointExceptSelf());
    }

    private void changeToRole(AbstractNodeRole newRole){
        logger.debug("node{}, role state changed -> {}", context.selfId(), newRole);

         // term & votedFor 重新设置
        NodeStore store = context.getStore();
        store.setTerm(newRole.getTerm());
        if(newRole.getName() == RoleName.FOLLOWER){
            store.setVotedFor(((FollowerNodeRole)newRole).getVotedFor());
        }
        role = newRole;
    }

    @Override
    public synchronized void stop() throws InterruptedException {
        if(!started){
            throw new IllegalStateException("node not started");
        }

        context.getScheduler().stop();
        context.getConnector().close();
        context.getTaskExecutor().shutdown();
        started = false;
    }

    /**
     * 收到投票请求之后, 在主线程中提交处理请求的任务
     *
     * @param rpcMessage 需要发送的 rpcMessage
     */
    @Subscribe
    public void onReceiveRequestVoteRpc(RequestVoteRpcMessage rpcMessage){
        context.getTaskExecutor().submit(
                () -> context.getConnector().replyRequestVote(
                        doProcessRequestVoteRpc(rpcMessage),
                        rpcMessage
                ), LOGGING_FUTURE_CALLBACK
        );
    }

    private RequestVoteResult doProcessRequestVoteRpc(RequestVoteRpcMessage rpcMessage){
        RequestVoteRpc rpc = (RequestVoteRpc) rpcMessage.getRpc();
        // 对方 term 小, 则不投票, 返回自己这个较大的term
        if(rpc.getTerm() < role.getTerm()){
            logger.debug("term form rpc < current term , don't vote ({} <{}", rpc.getTerm(), role.getTerm());
            return new RequestVoteResult(role.getTerm(), false);
        }

        boolean voteForCandidate = !context.getLog().isNewerThan(rpc.getLastLogIndex(), rpc.getLastLogTerm());;
        if(rpc.getTerm() > role.getTerm()){
            becomeFollower(rpc.getTerm(), (voteForCandidate ? rpc.getCandidateId() : null), null, true);
            return new RequestVoteResult(rpc.getTerm(), voteForCandidate);
        }

        switch ((role.getName())){
            case FOLLOWER:
                FollowerNodeRole followerNodeRole = (FollowerNodeRole) role;
                NodeId votedFor = followerNodeRole.getVotedFor();
                /*
                 * 当满足一下两种条件则投票:
                 * case1: 自己尚未投票, 并且对方的日志比自己新
                 * case2: 自己已经给对方投过票
                 * 投票后需要切换为 Follower 角色
                 */
                boolean ifVote = (votedFor == null && voteForCandidate)
                                || Objects.equals(votedFor, rpc.getCandidateId());
                if(ifVote){
                    becomeFollower(role.getTerm(), rpc.getCandidateId(), null, true);
                    return new RequestVoteResult(rpc.getTerm(), true);
                }
                return new RequestVoteResult(role.getTerm(), false);

            // 当为 candidate 时, 已经给自己投过票
            case CANDIDATE:

            case LEADER:
                return new RequestVoteResult(role.getTerm(), false);

            default:
                throw new IllegalStateException("unexpected node role [" + role.getName() + "]");
        }
    }

    private void becomeFollower(int term, NodeId votedFor, NodeId leaderId, boolean scheduleElectionTimeout){
        role.cancelTimeoutOrTask();
        if(leaderId != null && !leaderId.equals(role.getLeaderId(context.selfId()))){
            logger.debug("current leader is {}, term {}", leaderId, term);
        }
        ElectionTimeout electionTimeout = scheduleElectionTimeout ? scheduleElectionTimeout() : ElectionTimeout.NONE;
        changeToRole(new FollowerNodeRole(term, votedFor, leaderId, electionTimeout));
    }

    /**
     * 收到请求投票 vote 的回复
     * @param result RequestVoteResult
     */
    @Subscribe
    public void onReceiveRequestVoteResult(RequestVoteResult result){
        context.getTaskExecutor().submit(
                () -> doProcessRequestVoteResult(result), LOGGING_FUTURE_CALLBACK
        );
    }

    private void doProcessRequestVoteResult(RequestVoteResult result){
        // 对方的 term 比自己大, 则退化为 follower 角色
        if(result.getTerm() > role.getTerm()){
            becomeFollower(result.getTerm(), null, null,true);
        }

        // 如果自己不是 candidate 角色, 则忽略
        if(role.getName() != RoleName.CANDIDATE){
            logger.debug("receive request vote result and current role is not candidate, ignore");
            return;
        }

        // 如果对方的 term 比自己小或者对象没有给自己投票, 忽略
        if(result.getTerm() < role.getTerm() || !result.isVoteGranted()){
            return;
        }

        /* 响应中的 term 和本地一致, 并且被投票了那么进入下面的阶段 */

        // 当前收到的投票数
        int currentVoteCount = ((CandidateNodeRole) role).getVotesCount() + 1;
        // 总节点数
        int countOfMajor = context.getGroup().getCount();
        logger.debug("votes count {}, node count {}", currentVoteCount, countOfMajor);

        role.cancelTimeoutOrTask();
        // 得票过半
        if(currentVoteCount > countOfMajor / 2){
            logger.debug("become leader , term {}", role.getTerm());
            // 复位所有节点复制进度为 nextIndex
            resetReplicatingStates();
            changeToRole(new LeaderNodeRole(role.getTerm(), scheduleLogReplicationTask()));
            // NO-OP 日志
            context.getLog().appendEntry(role.getTerm());
            context.getConnector().resetChannels(); // close all inbound channels
        }else {
            // 继续等待更多票数, 不是通过 set term count timeout 而是 new 一个 candidateNodeRole
            changeToRole(new CandidateNodeRole(role.getTerm(), currentVoteCount, scheduleElectionTimeout()));
        }
    }

    private LogReplicationTask scheduleLogReplicationTask(){
        return context.getScheduler().scheduleLogReplicationTask(this::replicateLog);
    }

    /**
     * 成为 leader 之后的心跳消息
     * 主线程对所有节点挨个发送 appendEntries 消息
     */
    void replicateLog(){
        context.getTaskExecutor().submit(this::doReplicateLog, LOGGING_FUTURE_CALLBACK);
    }

    private void doReplicateLog(){
        logger.debug("replicate log");
        for(GroupMember member : context.getGroup().listReplicationTarget()){
            doSingleReplicateLog(member, context.getConfig().getMaxReplicationEntries());
        }
    }

    private void doSingleReplicateLog(GroupMember member, int maxEntries){
        AppendEntriesRpc rpc = context.getLog().createAppendEntriesRpc(
                role.getTerm(), context.selfId(), member.getNextIndex(), maxEntries);
        context.getConnector().sendAppendEntries(rpc, member.getEndpoint());
    }

    private void resetReplicatingStates() {
        context.getGroup().resetReplicatingStates(context.getLog().getNextIndex());
    }
    /**
     * 收到 leader 的心跳消息
     *
     * @param rpcMessage 收到的 rpcMessage
     */
    @Subscribe
    public void onReceiveAppendEntriesRpc(AppendEntriesRpcMessage rpcMessage){
        context.getTaskExecutor().submit(
                () -> context.getConnector().replyAppendEntries(
                        doProcessAppendEntriesRpc(rpcMessage),
                        rpcMessage
                ), LOGGING_FUTURE_CALLBACK
        );
    }

    private AppendEntriesResult doProcessAppendEntriesRpc(AppendEntriesRpcMessage rpcMessage){
        AppendEntriesRpc rpc = (AppendEntriesRpc) rpcMessage.getRpc();
        if(rpc.getTerm() < role.getTerm()){
            return new AppendEntriesResult(rpc.getMessageId(), role.getTerm(), false);
        }

        /* 对方 term 大于自己, 那么自己无论之前什么角色, 都变成 follower,
         * 设置对方为 leader , 尝试追加日志
         */
        if(rpc.getTerm() > role.getTerm()){
            becomeFollower(rpc.getTerm(), null, rpc.getLeaderId(), true);
            return new AppendEntriesResult(rpc.getMessageId(), rpc.getTerm(), appendEntries(rpc));
        }

        assert rpc.getTerm() == role.getTerm();
        switch (role.getName()){
            case FOLLOWER:
                becomeFollower(rpc.getTerm(), ((FollowerNodeRole)role).getVotedFor(), rpc.getLeaderId(), true);
                return new AppendEntriesResult(rpc.getMessageId(), rpc.getTerm(), appendEntries(rpc));

            // 退化为 follower, 阐释追加日志
            case CANDIDATE:
                becomeFollower(rpc.getTerm(), null, rpc.getLeaderId(), true);
                return new AppendEntriesResult(rpc.getMessageId(), rpc.getTerm(), appendEntries(rpc));

            case LEADER:
                logger.debug("receive append entries rpc from another leader {} , ignore", rpc.getLeaderId());
                return new AppendEntriesResult(rpc.getMessageId(), rpc.getTerm(), false);

            default:
                throw new IllegalStateException("unexpected node role [" + role.getName() + "]");
        }
    }

    private boolean appendEntries(AppendEntriesRpc rpc){
        // 尝试追加日志
        boolean result = context.getLog().appendEntriesFromLeader(rpc.getPrevLogIndex(), rpc.getPrevLogTerm(), rpc.getEntries());
        if (result) {
            // 追加日志成功则判断是否需要 commit index
            context.getLog().advanceCommitIndex(Math.min(rpc.getLeaderCommit(), rpc.getLastEntryIndex()), rpc.getTerm());
        }
        return result;
    }

    /**
     * 接受到 appendEntriesResult 之后的动作
     *
     * 如果收到的回应中的 term 大于本节点的 term 那么退成 Follower
     *
     * @param resultMessage 接受到的 appendEntries 回复消息
     */
    @Subscribe
    public void onReceiveAppendEntriesResult(AppendEntriesResultMessage resultMessage){
        context.getTaskExecutor().submit( () -> doProcessAppendEntriesResult(resultMessage), LOGGING_FUTURE_CALLBACK);
    }

    private void doProcessAppendEntriesResult(AppendEntriesResultMessage resultMessage){
        AppendEntriesResult result = resultMessage.get();
        if(result.getTerm() > role.getTerm()){
            becomeFollower(result.getTerm(), null, null, true);
            return;
        }

        if(role.getName() != RoleName.LEADER){
            logger.warn(String.format("receive append entries result from node %s but current node is not leader, ignore", resultMessage.getSourceNodeId()));
            return;
        }

        NodeId sourceNodeId = resultMessage.getSourceNodeId();
        GroupMember member = context.getGroup().getMember(sourceNodeId);
        // 没有相应成员
        if (member == null) {
            logger.info("unexpected append entries result from node {}, node maybe removed", sourceNodeId);
            return;
        }

        AppendEntriesRpc rpc = resultMessage.getRpc();
        // 收到成功回复
        if (result.isSuccess()) {
            if (member.advanceReplicatingState(rpc.getLastEntryIndex())) {
                context.getLog().advanceCommitIndex(context.getGroup().getMatchIndexOfMajor(), role.getTerm());
            }
        } else {
            if (!member.backOffNextIndex()) {
                logger.warn("cannot back off next index more, node {}", sourceNodeId);
            }
        }
    }
}
