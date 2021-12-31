package raft.kvstore.message;

import raft.kvstore.Protos;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.UUID;

public class SetCommand {
    private final String requestId;
    private final String key;
    private final byte[] value;

    public SetCommand(String key, byte[] value){
        this(UUID.randomUUID().toString(), key, value);
    }

    public SetCommand(String requestId, String key, byte[] value) {
        this.requestId = requestId;
        this.key = key;
        this.value = value;
    }

    public static SetCommand fromBytes(byte[] bytes){
        try{
            Protos.SetCommand protoCommand = Protos.SetCommand.parseFrom(bytes);
            return new SetCommand(
                    protoCommand.getRequestId(),
                    protoCommand.getKey(),
                    protoCommand.getValue().toByteArray()
            );
        }catch (InvalidProtocolBufferException e){
            throw new IllegalStateException("failed to deserialize set command", e);
        }
    }

    public byte[] toBytes(){
        return Protos.SetCommand.newBuilder()
                .setRequestId(this.requestId)
                .setKey(this.key)
                .setValue(ByteString.copyFrom(this.value))
                .build()
                .toByteArray();
    }

    public String getRequestId() { return requestId; }

    public String getKey() { return key; }

    public byte[] getValue() { return value; }

    @Override
    public String toString() {
        return "SetCommand{" +
                "requestId='" + requestId + '\'' +
                ", key='" + key + '\'' +
                '}';
    }
}
