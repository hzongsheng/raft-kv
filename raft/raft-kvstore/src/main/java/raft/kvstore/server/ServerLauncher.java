package raft.kvstore.server;

import com.sun.javafx.geom.transform.Identity;
import com.sun.org.apache.bcel.internal.classfile.ConstantLong;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.core.node.Node;
import raft.core.node.NodeBuilder;
import raft.core.node.NodeEndpoint;
import raft.core.node.NodeId;
import raft.core.rpc.Address;
import raft.kvstore.client.Console;

import javax.xml.ws.Endpoint;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerLauncher {

    private static final String MODE_STANDALONE = "standalone";
    private static final String MODE_STANDBY = "standby";
    private static final String MODE_GROUP_MEMBER = "group-member";

    private static final Logger logger = LoggerFactory.getLogger(ServerLauncher.class);
    /** 表示 -gc 参数项中节点的一共有几个信息, NodeId,localhost,raft_port,kv_port, 一共四个 */
    private static final int GC_TERMS = 4;

    private volatile Server server;

    private void execute(String[] args) throws Exception {

        Options options = new Options();

        options.addOption(Option.builder("m")
                .hasArg()
                .argName("mode")
                .desc("start mode, available: standalone, standby, group-member. default is standalone")
                .build());
        options.addOption(Option.builder("i")
                .longOpt("id")
                .hasArg()
                .argName("node-id")
                .required()
                .desc("node id, required. must be unique in group. " +
                        "if starts with mode group-member, please ensure id in group config")
                .build());
        options.addOption(Option.builder("h")
                .hasArg()
                .argName("host")
                .desc("host, required when starts with standalone or standby mode")
                .build());
        options.addOption(Option.builder("d")
                .hasArg()
                .argName("data-dir")
                .desc("data directory, optional. must be present")
                .build());
        options.addOption(Option.builder("c")
                .desc("running as a consloe or running as server")
                .build());
        options.addOption(Option.builder("gc")
                .hasArgs() // 注意复数
                .argName("node-endpoint")
                .desc("group config, required when starts with group-member mode. format: <node-endpoint> <node-endpoint>..., " +
                        "format of node-endpoint: <node-id>,<host>,<port-raft-node>,<port-kv-service>, eg: A,localhost,8000,9000 B,localhost,8010,9010")
                .build());

        if (args.length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("raft-kvstore [OPTION]...", options);
            return;
        }

        CommandLineParser parser = new DefaultParser();
        Map<NodeId, Address> serverMap;
        try {
            CommandLine cmdLine = parser.parse(options, args);

            // 运行为一个 console, 服务器形式启动, 因为 start() 将死循环处理输入, 不会去处理 IO
            if(cmdLine.hasOption("gc") && cmdLine.hasOption("c")) {
                serverMap = parseGroupConfig(cmdLine.getOptionValues("gc"));
                Console console = new Console(serverMap);
                console.start();
            }

            // 选择模式
            String mode = cmdLine.getOptionValue('m', MODE_STANDALONE);
            switch (mode) {
                case MODE_STANDBY:
                    startAsStandaloneOrStandby(cmdLine, true);
                    break;
                case MODE_STANDALONE:
                    startAsStandaloneOrStandby(cmdLine, false);
                    break;
                case MODE_GROUP_MEMBER:
                    startAsGroupMember(cmdLine);
                    break;
                default:
                    throw new IllegalArgumentException("illegal mode [" + mode + "]");
            }

        } catch (ParseException | IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }

    }

    /** 返回 NodeId 和 service port 的 map
     *
     * @param configs 节点配置 String 行
     * @return map
     */
    private Map<NodeId, Address> parseGroupConfig(String[] configs){
         return Stream.of(configs)
                .map(this::parseServerInfo)
                .collect(Collectors.toMap(s -> new NodeId(s.getStrId()), s -> new Address(s.getHost(), s.getServicePort())));
    }

    private void startAsStandaloneOrStandby(CommandLine cmdLine, boolean standby) throws Exception {
        // TODO: 2021/12/22 适应新参数形式

        if (!cmdLine.hasOption("p1") || !cmdLine.hasOption("p2")) {
            throw new IllegalArgumentException("port-raft-node or port-service required");
        }

        String id = cmdLine.getOptionValue('i');
        String host = cmdLine.getOptionValue('h', "localhost");
        int portRaftServer = ((Long) cmdLine.getParsedOptionValue("p1")).intValue();
        int portService = ((Long) cmdLine.getParsedOptionValue("p2")).intValue();

        NodeEndpoint nodeEndpoint = new NodeEndpoint(id, host, portRaftServer);
        Node node = new NodeBuilder(nodeEndpoint)
                .setStandby(standby)
                .setDataDir(cmdLine.getOptionValue('d'))
                .build();
        Server server = new Server(node, portService);
        logger.info("start with mode {}, id {}, host {}, port raft node {}, port service {}",
                (standby ? "standby" : "standalone"), id, host, portRaftServer, portService);
        startServer(server);
    }

    private void startAsGroupMember(CommandLine cmdLine) throws Exception {
        if (!cmdLine.hasOption("gc")) {
            throw new IllegalArgumentException("group-config required");
        }

        String rawNodeId = cmdLine.getOptionValue('i');
        String[] rawGroupConfig = cmdLine.getOptionValues("gc");

        Set<NodeEndpoint> endpoints = Stream.of(rawGroupConfig)
                .map(this::parseServerInfo)
                .map(s -> new NodeEndpoint(s.getStrId(), s.host, s.raftPort))
                .collect(Collectors.toSet());

        NodeId selfNodeId = new NodeId(rawNodeId);
        Node node = new NodeBuilder(endpoints, selfNodeId)
                .setDataDir(cmdLine.getOptionValue('d'))
                .build();

        Integer selfServicePort = Stream.of(rawGroupConfig)
                .map(this::parseServerInfo)
                .filter(s -> Objects.equals(s.getStrId(), rawNodeId))
                .map(serverInfo::getServicePort)
                .collect(Collectors.toList()).get(0);

        Server server = new Server(node, selfServicePort);
        logger.info("start as group member, group config {}, id {}", rawGroupConfig, rawNodeId );
        startServer(server);
    }

    private class serverInfo{
        private String strId;
        private String host;
        private Integer raftPort;
        private Integer servicePort;

        public serverInfo(String strId, String host, Integer raftPort, Integer servicePort) {
            this.strId = strId;
            this.host = host;
            this.raftPort = raftPort;
            this.servicePort = servicePort;
        }

        public String getStrId() {
            return strId;
        }

        public String getHost() {
            return host;
        }

        public Integer getRaftPort() {
            return raftPort;
        }

        public Integer getServicePort() {
            return servicePort;
        }
    }

    private serverInfo parseServerInfo(String rawNodeEndpoint) {
        String[] pieces = rawNodeEndpoint.split(",");
        if (pieces.length != GC_TERMS) {
            throw new IllegalArgumentException("illegal node endpoint [" + rawNodeEndpoint + "]");
        }
        String nodeId = pieces[0];
        String host = pieces[1];
        int raftPort, servicePort;
        try {
            raftPort = Integer.parseInt(pieces[2]);
            servicePort = Integer.parseInt(pieces[3]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("illegal port in node endpoint [" + rawNodeEndpoint + "]");
        }
        return new serverInfo(nodeId, host, raftPort, servicePort);
    }


    private void startServer(Server server) throws Exception {
        this.server = server;
        this.server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopServer, "shutdown"));
    }

    private void stopServer() {
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        ServerLauncher launcher = new ServerLauncher();
        launcher.execute(args);
    }

}
