package raft.core.schedule;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ElectionTimeout {

    /**
     * 没有任何作用的 ElectionTimeout
     */
    public static final ElectionTimeout NONE = new ElectionTimeout(new NullScheduledFuture());

    private final ScheduledFuture<?> scheduledFuture;

    public ElectionTimeout(ScheduledFuture<?> scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }

    public void cancel(){
        this.scheduledFuture.cancel(false);
    }

    @Override
    public String toString() {
        if(this.scheduledFuture.isCancelled()){
            return "ElectionTimeout(state=cancelled)";
        }

        if(this.scheduledFuture.isDone()){
            return "ElectionTimeout(state=done)";
        }

        return "ElectionTimeout{" +
                "delay=" + scheduledFuture.getDelay(TimeUnit.MILLISECONDS) + "ms" +
                '}';
    }
}
