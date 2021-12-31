package raft.kvstore.client;

public interface Command {

    /**
     * 命令名称
     *
     * @return String 命令名称
     */
    String getName();

    /**
     * Command 执行
     *
     * @param arguments 从命令行获取到的参数
     * @param context Command context
     */
    void execute (String arguments, CommandContext context);
}
