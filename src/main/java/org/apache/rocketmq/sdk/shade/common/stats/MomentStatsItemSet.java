package org.apache.rocketmq.sdk.shade.common.stats;

import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MomentStatsItemSet {
    private final ConcurrentMap<String, MomentStatsItem> statsItemTable = new ConcurrentHashMap(128);
    private final String statsName;
    private final ScheduledExecutorService scheduledExecutorService;
    private final InternalLogger log;

    public MomentStatsItemSet(String statsName, ScheduledExecutorService scheduledExecutorService, InternalLogger log) {
        this.statsName = statsName;
        this.scheduledExecutorService = scheduledExecutorService;
        this.log = log;
        init();
    }

    public ConcurrentMap<String, MomentStatsItem> getStatsItemTable() {
        return this.statsItemTable;
    }

    public String getStatsName() {
        return this.statsName;
    }

    public void init() {
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    MomentStatsItemSet.this.printAtMinutes();
                } catch (Throwable th) {
                }
            }
        }, Math.abs(UtilAll.computNextMinutesTimeMillis() - System.currentTimeMillis()), 300000, TimeUnit.MILLISECONDS);
    }

    public void printAtMinutes() {
        for (Map.Entry<String, MomentStatsItem> next : this.statsItemTable.entrySet()) {
            next.getValue().printAtMinutes();
        }
    }

    public void setValue(String statsKey, int value) {
        getAndCreateStatsItem(statsKey).getValue().set((long) value);
    }

    public MomentStatsItem getAndCreateStatsItem(String statsKey) {
        MomentStatsItem statsItem = this.statsItemTable.get(statsKey);
        if (null == statsItem) {
            statsItem = new MomentStatsItem(this.statsName, statsKey, this.scheduledExecutorService, this.log);
            if (null == this.statsItemTable.put(statsKey, statsItem)) {
            }
        }
        return statsItem;
    }
}
