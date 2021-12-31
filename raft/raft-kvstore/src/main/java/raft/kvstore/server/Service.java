package raft.kvstore.server;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;

import raft.core.log.statemachine.AbstractSingleThreadStateMachine;
import raft.core.node.Node;
import raft.core.node.role.RoleName;
import raft.core.node.role.RoleNameAndLeaderId;
import raft.kvstore.message.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Service {

    private static final Logger logger = LoggerFactory.getLogger(Service.class);

    private final Node node;

    private final ConcurrentMap<String, CommandRequest<?>> pendingCommands = new ConcurrentHashMap<>();
    private final Map<String, byte[]> map = new HashMap<>();

    public Service(Node node) {
        this.node = node;
        this.node.registerStateMachine(new StateMachineImpl());
    }

    /**
     * set 操作
     *
     * @param commandRequest 命令请求
     */
    public void set(CommandRequest<SetCommand> commandRequest) {
        Redirect redirect = checkLeadership();
        if (redirect != null) {
            // 回复重定向消息
            commandRequest.reply(redirect);
            return;
        }

        SetCommand command = commandRequest.getCommand();
        logger.debug("set {}", command.getKey());
        // 记录未 apply 的命令, 在 channel 关闭时删除这个 entry
        this.pendingCommands.put(command.getRequestId(), commandRequest);
        commandRequest.addCloseListener(() -> pendingCommands.remove(command.getRequestId()));
        this.node.appendLog(command.toBytes());
        // appendLog 成功之后 StateMachine 会 applyCommand
    }

    public void get(CommandRequest<GetCommand> commandRequest) {
        String key = commandRequest.getCommand().getKey();
        logger.debug("get {}", key);
        byte[] value = this.map.get(key);
        // 没有使用一个 No_Op 保证一致性, 直接返回
        commandRequest.reply(new GetCommandResponse(value));
    }

    private  Redirect checkLeadership(){
        RoleNameAndLeaderId state = node.getRoleNameAndLeaderId();
        if(state.getRoleName() != RoleName.LEADER){
            return new Redirect(state.getLeaderId());
        }
        return null;
    }


    private class StateMachineImpl extends AbstractSingleThreadStateMachine{
        /**
         * node appendLog 完之后回调 service applyLog.
         *
         * 反序列化命令 -> 执行修改 -> 查找链接返回结果
         *
         * @param commandBytes 二进制形式的命令
         */
        @Override
        protected void applyCommand(byte[] commandBytes) {
            // 反序列化命令
            SetCommand command = SetCommand.fromBytes(commandBytes);
            // 应用 key value
            map.put(command.getKey(), command.getValue());

            CommandRequest<?> commandRequest = pendingCommands.remove(command.getRequestId());
            if(commandRequest != null){
                commandRequest.reply(Success.INSTANCE);
            }
        }
    }
}
