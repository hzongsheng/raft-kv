package raft.core.rpc.nio;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import raft.core.Protos;
import raft.core.log.sequence.EntryFactory;
import raft.core.node.NodeId;
import raft.core.rpc.message.*;

import java.util.List;
import java.util.stream.Collectors;

public class Decoder extends ByteToMessageDecoder {

    private final EntryFactory entryFactory = new EntryFactory();
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 可读字节少于 8 个, 无法判断类型和长度
        int availableBytes = in.readableBytes();
        if (availableBytes < 8) {
            return;
        }

        // 记录下类型和长度信息, 之后根据此等待负载数据
        in.markReaderIndex();
        int messageType = in.readInt();
        int payloadLength = in.readInt();
        if(in.readableBytes() < payloadLength){
            in.resetReaderIndex();
            return;
        }

        // 读取负载数据
        byte[] payload = new byte[payloadLength];
        in.readBytes(payload);

        // 根据消息反序列化
        switch (messageType){
            case MessageConstants .MSG_TYPE_NODE_ID:
                out.add(new NodeId(new String(payload)));
                break;

            case MessageConstants.MSG_TYPE_REQUEST_VOTE_RPC:
                Protos.RequestVoteRpc protoRpc = Protos.RequestVoteRpc.parseFrom(payload);
                RequestVoteRpc rpc = new RequestVoteRpc();
                rpc.setTerm(protoRpc.getTerm());
                rpc.setCandidateId(new NodeId(protoRpc.getCandidateId()));
                rpc.setLastLogTerm(protoRpc.getLastLogTerm());
                rpc.setLastLogIndex(protoRpc.getLastLogIndex());
                out.add(rpc);
                break;

             case MessageConstants.MSG_TYPE_REQUEST_VOTE_RESULT:
                Protos.RequestVoteResult protoRVResult = Protos.RequestVoteResult.parseFrom(payload);
                out.add(new RequestVoteResult(protoRVResult.getTerm(), protoRVResult.getVoteGranted()));
                break;

            case MessageConstants.MSG_TYPE_APPEND_ENTRIES_RPC:
                Protos.AppendEntriesRpc protoAERpc = Protos.AppendEntriesRpc.parseFrom(payload);
                AppendEntriesRpc aeRpc = new AppendEntriesRpc();
                aeRpc.setMessageId(protoAERpc.getMessageId());
                aeRpc.setTerm(protoAERpc.getTerm());
                aeRpc.setLeaderId(new NodeId(protoAERpc.getLeaderId()));
                aeRpc.setLeaderCommit(protoAERpc.getLeaderCommit());
                aeRpc.setPrevLogIndex(protoAERpc.getPrevLogIndex());
                aeRpc.setPrevLogTerm(protoAERpc.getPrevLogTerm());
                aeRpc.setEntries(protoAERpc.getEntriesList().stream().map(e ->
                        entryFactory.create(e.getKind(), e.getIndex(), e.getTerm(), e.getCommand().toByteArray())
                ).collect(Collectors.toList()));
                out.add(aeRpc);
                break;

            case MessageConstants.MSG_TYPE_APPEND_ENTRIES_RESULT:
                Protos.AppendEntriesResult protoAEResult = Protos.AppendEntriesResult.parseFrom(payload);
                out.add(new AppendEntriesResult(protoAEResult.getRpcMessageId(), protoAEResult.getTerm(), protoAEResult.getSuccess()));
                break;

            default:
                throw new IllegalStateException("unknown type of message");
        }
    }
}
