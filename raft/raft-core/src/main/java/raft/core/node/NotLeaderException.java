package raft.core.node;

import com.google.common.base.Preconditions;
import raft.core.node.role.RoleName;

public class NotLeaderException extends RuntimeException {

    private final RoleName roleName;
    private final NodeEndpoint leaderEndpoint;

    /**
     * Create.
     *
     * @param roleName       role name
     * @param leaderEndpoint leader endpoint
     */
    public NotLeaderException( RoleName roleName, NodeEndpoint leaderEndpoint) {
        super("not leader");
        Preconditions.checkNotNull(roleName);
        this.roleName = roleName;
        this.leaderEndpoint = leaderEndpoint;
    }

    /**
     * Get role name.
     *
     * @return role name
     */
    public RoleName getRoleName() {
        return roleName;
    }

    /**
     * Get leader endpoint.
     *
     * @return leader endpoint
     */
    public NodeEndpoint getLeaderEndpoint() {
        return leaderEndpoint;
    }

}

