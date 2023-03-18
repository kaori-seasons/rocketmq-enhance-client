package org.apache.rocketmq.sdk.shade.common;

import org.apache.rocketmq.sdk.shade.common.constant.LoggerName;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ServiceThread implements Runnable {
    private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.COMMON_LOGGER_NAME);
    private static final long JOIN_TIME = 90000;
    protected final CountDownLatch2 waitPoint = new CountDownLatch2(1);
    protected volatile AtomicBoolean hasNotified = new AtomicBoolean(false);
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
        if (this.hasNotified.compareAndSet(false, true)) {
            this.waitPoint.countDown();
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

    public void stop() {
        stop(false);
    }

    public void stop(boolean interrupt) {
        this.stopped = true;
        log.info("stop thread " + getServiceName() + " interrupt " + interrupt);
        if (this.hasNotified.compareAndSet(false, true)) {
            this.waitPoint.countDown();
        }
        if (interrupt) {
            this.thread.interrupt();
        }
    }

    public void makeStop() {
        this.stopped = true;
        log.info("makestop thread " + getServiceName());
    }

    public void wakeup() {
        if (this.hasNotified.compareAndSet(false, true)) {
            this.waitPoint.countDown();
        }
    }

    protected void waitForRunning(long interval) {
        if (this.hasNotified.compareAndSet(true, false)) {
            onWaitEnd();
            return;
        }
        this.waitPoint.reset();
        try {
            try {
                this.waitPoint.await(interval, TimeUnit.MILLISECONDS);
                this.hasNotified.set(false);
                onWaitEnd();
            } catch (InterruptedException e) {
                log.error("Interrupted", (Throwable) e);
                this.hasNotified.set(false);
                onWaitEnd();
            }
        } catch (Throwable th) {
            this.hasNotified.set(false);
            onWaitEnd();
            throw th;
        }
    }

    protected void onWaitEnd() {
    }

    public boolean isStopped() {
        return this.stopped;
    }
}
