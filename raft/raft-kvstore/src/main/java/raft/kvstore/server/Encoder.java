package raft.kvstore.server;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import raft.kvstore.MessageConstants;
import raft.kvstore.Protos;
import raft.kvstore.message.*;

import java.io.IOException;

public class Encoder extends MessageToByteEncoder<Object> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {

        // 成功
        if (msg instanceof Success) {
            this.writeMessage(MessageConstants.MSG_TYPE_SUCCESS, Protos.Success.newBuilder().build(), out);
        }

        // 失败
        else if (msg instanceof Failure) {
            Failure failure = (Failure) msg;
            Protos.Failure protoFailure = Protos.Failure.newBuilder().setErrorCode(failure.getErrorCode()).setMessage(failure.getMessage()).build();
            this.writeMessage(MessageConstants.MSG_TYPE_FAILURE, protoFailure, out);
        }

        // 重定向
        else if (msg instanceof Redirect) {
            Redirect redirect = (Redirect) msg;
            Protos.Redirect protoRedirect = Protos.Redirect.newBuilder().setLeaderId(redirect.getLeaderId()).build();
            this.writeMessage(MessageConstants.MSG_TYPE_REDIRECT, protoRedirect, out);
        }

        // GET 命令
        else if (msg instanceof GetCommand) {
            GetCommand command = (GetCommand) msg;
            Protos.GetCommand protoGetCommand = Protos.GetCommand.newBuilder().setKey(command.getKey()).build();
            this.writeMessage(MessageConstants.MSG_TYPE_GET_COMMAND, protoGetCommand, out);
        }

        // GET 命令的回复
        else if (msg instanceof GetCommandResponse) {
            GetCommandResponse response = (GetCommandResponse) msg;
            byte[] value = response.getValue();
            Protos.GetCommandResponse protoResponse = Protos.GetCommandResponse.newBuilder()
                    .setFound(response.isFound())
                    .setValue(value != null ? ByteString.copyFrom(value) : ByteString.EMPTY).build();
            this.writeMessage(MessageConstants.MSG_TYPE_GET_COMMAND_RESPONSE, protoResponse, out);
        }

        // SET 命令
        else if (msg instanceof SetCommand) {
            SetCommand command = (SetCommand) msg;
            Protos.SetCommand protoSetCommand = Protos.SetCommand.newBuilder()
                    .setKey(command.getKey())
                    .setValue(ByteString.copyFrom(command.getValue()))
                    .build();
            this.writeMessage(MessageConstants.MSG_TYPE_SET_COMMAND, protoSetCommand, out);
        }
    }

    private void writeMessage(int messageType, MessageLite message, ByteBuf out) throws IOException {
         /* 二进制消息格式 :
          * 类型 | 长度 | 具体内容
         */
        out.writeInt(messageType);
        byte[] bytes = message.toByteArray();
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }

}
