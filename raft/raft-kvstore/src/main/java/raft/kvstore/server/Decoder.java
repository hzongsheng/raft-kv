package raft.kvstore.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import raft.kvstore.MessageConstants;
import raft.kvstore.Protos;
import raft.kvstore.message.*;

import java.util.List;

public class Decoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        if (in.readableBytes() < 2 * Integer.SIZE/8) {
            return;
        }

        in.markReaderIndex();
        int messageType = in.readInt();
        int payloadLength = in.readInt();
        if (in.readableBytes() < payloadLength) {
            in.resetReaderIndex();
            return;
        }

        byte[] payload = new byte[payloadLength];

        in.readBytes(payload);
        switch (messageType) {

            // 成功
            case MessageConstants.MSG_TYPE_SUCCESS:
                out.add(Success.INSTANCE);
                break;

            // 失败
            case MessageConstants.MSG_TYPE_FAILURE:
                Protos.Failure protoFailure = Protos.Failure.parseFrom(payload);
                out.add(new Failure(protoFailure.getErrorCode(), protoFailure.getMessage()));
                break;

            // 重定向
            case MessageConstants.MSG_TYPE_REDIRECT:
                Protos.Redirect protoRedirect = Protos.Redirect.parseFrom(payload);
                out.add(new Redirect(protoRedirect.getLeaderId()));
                break;

            // GET
            case MessageConstants.MSG_TYPE_GET_COMMAND:
                Protos.GetCommand protoGetCommand = Protos.GetCommand.parseFrom(payload);
                out.add(new GetCommand(protoGetCommand.getKey()));
                break;

            // GET 回复
            case MessageConstants.MSG_TYPE_GET_COMMAND_RESPONSE:
                Protos.GetCommandResponse protoGetCommandResponse = Protos.GetCommandResponse.parseFrom(payload);
                out.add(new GetCommandResponse(protoGetCommandResponse.getFound(), protoGetCommandResponse.getValue().toByteArray()));
                break;

            // SET
            case MessageConstants.MSG_TYPE_SET_COMMAND:
                Protos.SetCommand protoSetCommand = Protos.SetCommand.parseFrom(payload);
                out.add(new SetCommand(protoSetCommand.getKey(), protoSetCommand.getValue().toByteArray()));
                break;

            default:
                throw new IllegalStateException("unexpected message type " + messageType);
        }
    }
}
