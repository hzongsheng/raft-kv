package raft.kvstore.client;

public class ExitCommand implements Command{

    @Override
    public String getName() {
        return "exit";
    }

    @Override
    public void execute(String arguments, CommandContext context) {
        System.out.println("bye");
        context.setRunning(false);
        // client 使用的 channel 是 SocketChannel, 这里面只存储了地址和端口, 每次使用时建立一次性的 Channel
        // 所以无需关闭
    }
}
