package raft.core.schedule;

import javax.swing.plaf.multi.MultiMenuBarUI;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LogReplicationTask {

    public static final LogReplicationTask NONE = new LogReplicationTask(new NullScheduledFuture());
    private final ScheduledFuture<?> scheduledFuture;

    public LogReplicationTask(ScheduledFuture<?> scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }

    public void cancel(){
        this.scheduledFuture.cancel(false);
    }

    @Override
    public String toString() {
        return "LogReplicationTask{" +
                "scheduledFuture=" + scheduledFuture.getDelay(TimeUnit.MILLISECONDS) +
                '}';
    }
}
