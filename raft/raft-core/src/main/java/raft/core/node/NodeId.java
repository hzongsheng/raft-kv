package raft.core.node;

import java.io.Serializable;
import java.util.Objects;

public class NodeId implements Serializable {
    private final String value;

    public NodeId(String value){
        this.value = value;
    }

    public static NodeId of(String value){return new NodeId(value);}

    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(!(o instanceof NodeId))return false;
        NodeId nodeId = (NodeId) o;
        return Objects.equals(value, nodeId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return this.value;
    }
}
