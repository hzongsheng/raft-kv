package raft.kvstore.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import raft.core.node.NodeId;
import raft.core.rpc.Address;
import raft.core.service.Channel;
import raft.kvstore.MessageConstants;
import raft.kvstore.Protos;
import raft.kvstore.message.GetCommand;
import raft.kvstore.message.SetCommand;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class SocketChannel implements Channel {
    private final String host;
    private final int port;

    public SocketChannel(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public Object send(Object payload){
        // try-resource 语法, 退出代码块自动关闭 socket
        try(Socket socket = new Socket()){
            socket.setTcpNoDelay(true);
            InetSocketAddress addr = new InetSocketAddress(this.host, this.port);
            socket.connect(addr);

            // socketChannel Encode 对象, 写到 socket OutputStream 中
            this.write(socket.getOutputStream(), payload);

            // socketChannel Decode 字节流, 返回
            return this.read(socket.getInputStream());
        }catch (Exception e){
            throw new ChannelException("failed to send and receive ", e);
        }
    }

   private Object read(InputStream input) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(input);
        int messageType = dataInputStream.readInt();
        int payloadLength = dataInputStream.readInt();
        byte[] payload = new byte[payloadLength];
        dataInputStream.readFully(payload);

        switch (messageType){
            case MessageConstants.MSG_TYPE_SUCCESS:
                return null;

            case MessageConstants.MSG_TYPE_FAILURE:
                Protos.Failure protoFailure =  Protos.Failure.parseFrom(payload);
                throw new ChannelException("error code "+ protoFailure.getErrorCode() + ", message " + protoFailure.getMessage());

            // 重定向, 创建新的携带目标 id 的 Exception
            case MessageConstants.MSG_TYPE_REDIRECT:
                Protos.Redirect protoRedirect = Protos.Redirect.parseFrom(payload);
                throw new RedirectException(new NodeId(protoRedirect.getLeaderId()));

            case MessageConstants.MSG_TYPE_GET_COMMAND_RESPONSE:
                Protos.GetCommandResponse protoGetCommandResponse = Protos.GetCommandResponse.parseFrom(payload);
                if (!protoGetCommandResponse.getFound()) {
                    return null;
                }
                return protoGetCommandResponse.getValue().toByteArray();

            default:
                throw new ChannelException("unexpected message type " + messageType);
        }
   }

    private void write(OutputStream output, Object payload) throws IOException {
        if (payload instanceof GetCommand) {
            Protos.GetCommand protoGetCommand = Protos.GetCommand.newBuilder().setKey(((GetCommand) payload).getKey()).build();
            this.writeAppendInfo(output, MessageConstants.MSG_TYPE_GET_COMMAND, protoGetCommand);
        }

        else if (payload instanceof SetCommand) {
            SetCommand setCommand = (SetCommand) payload;
            Protos.SetCommand protoSetCommand = Protos.SetCommand.newBuilder()
                    .setKey(setCommand.getKey())
                    .setValue(ByteString.copyFrom(setCommand.getValue())).build();
            this.writeAppendInfo(output, MessageConstants.MSG_TYPE_SET_COMMAND, protoSetCommand);
        }
    }

    private void writeAppendInfo(OutputStream output, int messageType, MessageLite message) throws IOException {
        DataOutputStream dataOutput = new DataOutputStream(output);
        byte[] messageBytes = message.toByteArray();
        dataOutput.writeInt(messageType);
        dataOutput.writeInt(messageBytes.length);
        dataOutput.write(messageBytes);
        dataOutput.flush();
    }
}
