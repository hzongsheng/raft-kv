package raft.kvstore.client;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import raft.core.node.NodeId;
import raft.core.rpc.Address;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Console {

    private static final String PROMPT = "kvstore-client " + Client.VERSION + "> ";
    private final Map<String, Command> commandMap;
    private final CommandContext commandContext;
    private final LineReader reader;

    public Console(Map<NodeId, Address> serverMap) {
        commandMap = buildCommandMap(Arrays.asList(
                new ExitCommand(),
                new ClientSetLeaderCommand(),
                new KVStoreGetCommand(),
                new KVStoreSetCommand()
        ));
        commandContext = new CommandContext(serverMap);

        ArgumentCompleter completer = new ArgumentCompleter(
                new StringsCompleter(commandMap.keySet()),
                new NullCompleter()
        );
        reader = LineReaderBuilder.builder()
                .completer(completer)
                .build();
    }

    private Map<String, Command> buildCommandMap(Collection<Command> commands) {
        Map<String, Command> commandMap = new HashMap<>();
        for (Command cmd : commands) {
            commandMap.put(cmd.getName(), cmd);
        }
        return commandMap;
    }

    public void start() {
        commandContext.setRunning(true);
        showInfo();
        String line;
        while (commandContext.isRunning()) {
            try {
                line = reader.readLine(PROMPT);
                if (line.trim().isEmpty()) {
                    continue;
                }
                dispatchCommand(line);
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
            } catch (EndOfFileException ignored) {
                break;
            }
        }
    }

    private void showInfo() {
        System.out.println("Welcome to Raft KVStore Shell\n");
        System.out.println("***********************************************");
        System.out.println("current server list: \n");
        commandContext.printSeverList();
        System.out.println("***********************************************");
    }

    /**
     * 解析命令, 并在 commandMap 中查找对应的类执行
     *
     * @param line String 命令行命令
     */
    private void dispatchCommand(String line) {
        String[] commandNameAndArguments = line.split("\\s+", 2);
        String commandName = commandNameAndArguments[0];
        Command command = commandMap.get(commandName);
        if (command == null) {
            throw new IllegalArgumentException("no such command [" + commandName + "]");
        }
        // 解析命令和参数, 执行
        String str = commandNameAndArguments.length > 1 ? commandNameAndArguments[1] : "";
        command.execute(str, commandContext);
    }

}
