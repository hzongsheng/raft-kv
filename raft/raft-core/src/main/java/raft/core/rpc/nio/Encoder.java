package raft.core.rpc.nio;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import raft.core.Protos;
import raft.core.node.NodeId;
import raft.core.rpc.message.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;

public class Encoder extends MessageToByteEncoder<Object> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if(msg instanceof NodeId) {
            this.writeMessage(out, MessageConstants.MSG_TYPE_NODE_ID, ((NodeId) msg).getValue().getBytes());
        }else if(msg instanceof RequestVoteRpc){
            RequestVoteRpc rpc = (RequestVoteRpc) msg;
            Protos.RequestVoteRpc protoRpc = Protos.RequestVoteRpc.newBuilder()
                    .setTerm(rpc.getTerm())
                    .setCandidateId(rpc.getCandidateId().getValue())
                    .setLastLogIndex(rpc.getLastLogIndex())
                    .setLastLogTerm(rpc.getLastLogTerm())
                    .build();
            this.writeMessage(out, MessageConstants.MSG_TYPE_REQUEST_VOTE_RPC, protoRpc);
        }
        else if (msg instanceof RequestVoteResult) {
            RequestVoteResult result = (RequestVoteResult) msg;
            Protos.RequestVoteResult protoResult = Protos.RequestVoteResult.newBuilder()
                    .setTerm(result.getTerm())
                    .setVoteGranted(result.isVoteGranted())
                    .build();
            this.writeMessage(out, MessageConstants.MSG_TYPE_REQUEST_VOTE_RESULT, protoResult);
        }
        else if (msg instanceof AppendEntriesRpc) {
            AppendEntriesRpc rpc = (AppendEntriesRpc) msg;
            Protos.AppendEntriesRpc protoRpc = Protos.AppendEntriesRpc.newBuilder()
                    .setMessageId(rpc.getMessageId())
                    .setTerm(rpc.getTerm())
                    .setLeaderId(rpc.getLeaderId().getValue())
                    .setLeaderCommit(rpc.getLeaderCommit())
                    .setPrevLogIndex(rpc.getPrevLogIndex())
                    .setPrevLogTerm(rpc.getPrevLogTerm())
                    .addAllEntries(
                            rpc.getEntries().stream().map(e ->
                                    Protos.AppendEntriesRpc.Entry.newBuilder()
                                            .setKind(e.getKind())
                                            .setIndex(e.getIndex())
                                            .setTerm(e.getTerm())
                                            .setCommand(ByteString.copyFrom(e.getCommandBytes()))
                                            .build()
                            ).collect(Collectors.toList())
                    ).build();

            this.writeMessage(out, MessageConstants.MSG_TYPE_APPEND_ENTRIES_RPC, protoRpc);
        }
        else if (msg instanceof AppendEntriesResult) {
            AppendEntriesResult result = (AppendEntriesResult) msg;
            Protos.AppendEntriesResult protoResult = Protos.AppendEntriesResult.newBuilder()
                    .setRpcMessageId(result.getRpcMessageId())
                    .setTerm(result.getTerm())
                    .setSuccess(result.isSuccess())
                    .build();
            this.writeMessage(out, MessageConstants.MSG_TYPE_APPEND_ENTRIES_RESULT, protoResult);
        }
    }

    /**
     * ??? protobuf ?????????, ??? RequestVoteRpc ???
     * ?????? ???????????? + ?????? + protobuf ????????????
     * @param out out buffer
     * @param messageType ????????????
     * @param message ??????
     * @throws IOException ?????? IO ??????
     */
    private void writeMessage(ByteBuf out, int messageType, MessageLite message) throws IOException {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        message.writeTo(byteOutput);
        out.writeInt(messageType);
        this.writeMessage(out, byteOutput.toByteArray());
    }

    /**
     * ?????? NodeId ?????????????????????????????????????????????
     * ?????? ???????????? + ???????????? + ???????????????????????????
     * @param out out buffer
     * @param messageType ????????????
     * @param bytes ????????????
     */
    private void writeMessage(ByteBuf out, int messageType, byte[] bytes){
        out.writeInt(messageType);
        this.writeMessage(out, bytes);
    }

    /**
     * ?????? ???????????? ??? ????????????
     * @param out out buffer
     * @param bytes ??????????????????
     */
    private void writeMessage(ByteBuf out, byte[] bytes){
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}
