package org.apache.rocketmq.sdk.shade.remoting.common;

import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;

public abstract class ServiceThread implements Runnable {
    private static final InternalLogger log = InternalLoggerFactory.getLogger(RemotingHelper.ROCKETMQ_REMOTING);
    private static final long JOIN_TIME = 90000;
    protected volatile boolean hasNotified = false;
    protected volatile boolean stopped = false;
    protected final Thread thread = new Thread(this, getServiceName());

    public abstract String getServiceName();

    public void start() {
        this.thread.start();
    }

    public void shutdown() {
        shutdown(false);
    }

    public void shutdown(boolean interrupt) {
        this.stopped = true;
        log.info("shutdown thread " + getServiceName() + " interrupt " + interrupt);
        synchronized (this) {
            if (!this.hasNotified) {
                this.hasNotified = true;
                notify();
            }
        }
        try {
            if (interrupt) {
                this.thread.interrupt();
            }

            long beginTime = System.currentTimeMillis();
            if (!this.thread.isDaemon()) {
                this.thread.join(getJointime());
            }
            long eclipseTime = System.currentTimeMillis() - beginTime;
            log.info("join thread " + getServiceName() + " eclipse time(ms) " + eclipseTime + " " +
                getJointime());
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
        }
    }

    public long getJointime() {
        return JOIN_TIME;
    }

    public boolean isStopped() {
        return this.stopped;
    }
}
