package org.apache.rocketmq.sdk.shade.common.stats;

import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class MomentStatsItem {
    private final AtomicLong value = new AtomicLong(0);
    private final String statsName;
    private final String statsKey;
    private final ScheduledExecutorService scheduledExecutorService;
    private final InternalLogger log;

    public MomentStatsItem(String statsName, String statsKey, ScheduledExecutorService scheduledExecutorService, InternalLogger log) {
        this.statsName = statsName;
        this.statsKey = statsKey;
        this.scheduledExecutorService = scheduledExecutorService;
        this.log = log;
    }

    public void init() {
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    MomentStatsItem.this.printAtMinutes();
                    MomentStatsItem.this.value.set(0);
                } catch (Throwable th) {
                }
            }
        }, Math.abs(UtilAll.computNextMinutesTimeMillis() - System.currentTimeMillis()), 300000, TimeUnit.MILLISECONDS);
    }

    public void printAtMinutes() {
        this.log.info(String.format("[%s] [%s] Stats Every 5 Minutes, Value: %d", this.statsName, this.statsKey, Long.valueOf(this.value.get())));
    }

    public AtomicLong getValue() {
        return this.value;
    }

    public String getStatsKey() {
        return this.statsKey;
    }

    public String getStatsName() {
        return this.statsName;
    }
}
