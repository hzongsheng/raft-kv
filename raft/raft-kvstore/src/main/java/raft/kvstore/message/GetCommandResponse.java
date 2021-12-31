package raft.kvstore.message;

public class GetCommandResponse {
    /** 为了支持 value 为 null 的 kv 服务 , 所以需要用一个值表示是否有返回值, 而不是直接用 null 会产生歧义 */
    private final boolean found;
    private final byte[] value;

    public GetCommandResponse(byte[] value) {
        this(value != null, value);
    }

    public GetCommandResponse(boolean found, byte[] value) {
        this.found = found;
        this.value = value;
    }

    public boolean isFound() {
        return found;
    }

    public byte[] getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "GetCommandResponse{" +
                "found=" + found +
                '}';
    }
}
