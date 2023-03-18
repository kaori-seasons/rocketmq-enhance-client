package org.apache.rocketmq.sdk.shade.common.stats;

import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.DateUtils;

public class StatsItemSet {
    private final ConcurrentMap<String, StatsItem> statsItemTable = new ConcurrentHashMap(128);
    private final String statsName;
    private final ScheduledExecutorService scheduledExecutorService;
    private final InternalLogger log;

    public StatsItemSet(String statsName, ScheduledExecutorService scheduledExecutorService, InternalLogger log) {
        this.statsName = statsName;
        this.scheduledExecutorService = scheduledExecutorService;
        this.log = log;
        init();
    }

    public void init() {
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    StatsItemSet.this.samplingInSeconds();
                } catch (Throwable th) {
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    StatsItemSet.this.samplingInMinutes();
                } catch (Throwable th) {
                }
            }
        }, 0, 10, TimeUnit.MINUTES);
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    StatsItemSet.this.samplingInHour();
                } catch (Throwable th) {
                }
            }
        }, 0, 1, TimeUnit.HOURS);
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    StatsItemSet.this.printAtMinutes();
                } catch (Throwable th) {
                }
            }
        }, Math.abs(UtilAll.computNextMinutesTimeMillis() - System.currentTimeMillis()), 60000, TimeUnit.MILLISECONDS);
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    StatsItemSet.this.printAtHour();
                } catch (Throwable th) {
                }
            }
        }, Math.abs(UtilAll.computNextHourTimeMillis() - System.currentTimeMillis()), DateUtils.MILLIS_PER_HOUR, TimeUnit.MILLISECONDS);
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    StatsItemSet.this.printAtDay();
                } catch (Throwable th) {
                }
            }
        }, Math.abs(UtilAll.computNextMorningTimeMillis() - System.currentTimeMillis()), DateUtils.MILLIS_PER_DAY, TimeUnit.MILLISECONDS);
    }

    public void samplingInSeconds() {
        for (Map.Entry<String, StatsItem> next : this.statsItemTable.entrySet()) {
            next.getValue().samplingInSeconds();
        }
    }

    public void samplingInMinutes() {
        for (Map.Entry<String, StatsItem> next : this.statsItemTable.entrySet()) {
            next.getValue().samplingInMinutes();
        }
    }

    public void samplingInHour() {
        for (Map.Entry<String, StatsItem> next : this.statsItemTable.entrySet()) {
            next.getValue().samplingInHour();
        }
    }

    public void printAtMinutes() {
        for (Map.Entry<String, StatsItem> next : this.statsItemTable.entrySet()) {
            next.getValue().printAtMinutes();
        }
    }

    public void printAtHour() {
        for (Map.Entry<String, StatsItem> next : this.statsItemTable.entrySet()) {
            next.getValue().printAtHour();
        }
    }

    public void printAtDay() {
        for (Map.Entry<String, StatsItem> next : this.statsItemTable.entrySet()) {
            next.getValue().printAtDay();
        }
    }

    public void addValue(String statsKey, int incValue, int incTimes) {
        StatsItem statsItem = getAndCreateStatsItem(statsKey);
        statsItem.getValue().addAndGet((long) incValue);
        statsItem.getTimes().addAndGet((long) incTimes);
    }

    public StatsItem getAndCreateStatsItem(String statsKey) {
        StatsItem statsItem = this.statsItemTable.get(statsKey);
        if (null == statsItem) {
            statsItem = new StatsItem(this.statsName, statsKey, this.scheduledExecutorService, this.log);
            if (null == this.statsItemTable.put(statsKey, statsItem)) {
            }
        }
        return statsItem;
    }

    public StatsSnapshot getStatsDataInMinute(String statsKey) {
        StatsItem statsItem = this.statsItemTable.get(statsKey);
        if (null != statsItem) {
            return statsItem.getStatsDataInMinute();
        }
        return new StatsSnapshot();
    }

    public StatsSnapshot getStatsDataInHour(String statsKey) {
        StatsItem statsItem = this.statsItemTable.get(statsKey);
        if (null != statsItem) {
            return statsItem.getStatsDataInHour();
        }
        return new StatsSnapshot();
    }

    public StatsSnapshot getStatsDataInDay(String statsKey) {
        StatsItem statsItem = this.statsItemTable.get(statsKey);
        if (null != statsItem) {
            return statsItem.getStatsDataInDay();
        }
        return new StatsSnapshot();
    }

    public StatsItem getStatsItem(String statsKey) {
        return this.statsItemTable.get(statsKey);
    }
}
