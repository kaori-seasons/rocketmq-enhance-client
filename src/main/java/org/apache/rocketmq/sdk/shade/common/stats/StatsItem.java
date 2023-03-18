package org.apache.rocketmq.sdk.shade.common.stats;

import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class StatsItem {
    private final AtomicLong value = new AtomicLong(0L);
    private final AtomicLong times = new AtomicLong(0L);
    private final LinkedList<CallSnapshot> csListMinute = new LinkedList();
    private final LinkedList<CallSnapshot> csListHour = new LinkedList();
    private final LinkedList<CallSnapshot> csListDay = new LinkedList();
    private final String statsName;
    private final String statsKey;
    private final ScheduledExecutorService scheduledExecutorService;
    private final InternalLogger log;

    public StatsItem(String statsName, String statsKey, ScheduledExecutorService scheduledExecutorService, InternalLogger log) {
        this.statsName = statsName;
        this.statsKey = statsKey;
        this.scheduledExecutorService = scheduledExecutorService;
        this.log = log;
    }

    private static StatsSnapshot computeStatsData(LinkedList<CallSnapshot> csList) {
        StatsSnapshot statsSnapshot = new StatsSnapshot();
        synchronized(csList) {
            double tps = 0.0D;
            double avgpt = 0.0D;
            long sum = 0L;
            if (!csList.isEmpty()) {
                CallSnapshot first = (CallSnapshot)csList.getFirst();
                CallSnapshot last = (CallSnapshot)csList.getLast();
                sum = last.getValue() - first.getValue();
                tps = (double)sum * 1000.0D / (double)(last.getTimestamp() - first.getTimestamp());
                long timesDiff = last.getTimes() - first.getTimes();
                if (timesDiff > 0L) {
                    avgpt = (double)sum * 1.0D / (double)timesDiff;
                }
            }

            statsSnapshot.setSum(sum);
            statsSnapshot.setTps(tps);
            statsSnapshot.setAvgpt(avgpt);
            return statsSnapshot;
        }
    }

    public StatsSnapshot getStatsDataInMinute() {
        return computeStatsData(this.csListMinute);
    }

    public StatsSnapshot getStatsDataInHour() {
        return computeStatsData(this.csListHour);
    }

    public StatsSnapshot getStatsDataInDay() {
        return computeStatsData(this.csListDay);
    }

    public void init() {
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    StatsItem.this.samplingInSeconds();
                } catch (Throwable var2) {
                }

            }
        }, 0L, 10L, TimeUnit.SECONDS);
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    StatsItem.this.samplingInMinutes();
                } catch (Throwable var2) {
                }

            }
        }, 0L, 10L, TimeUnit.MINUTES);
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    StatsItem.this.samplingInHour();
                } catch (Throwable var2) {
                }

            }
        }, 0L, 1L, TimeUnit.HOURS);
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    StatsItem.this.printAtMinutes();
                } catch (Throwable var2) {
                }

            }
        }, Math.abs(UtilAll.computNextMinutesTimeMillis() - System.currentTimeMillis()), 60000L, TimeUnit.MILLISECONDS);
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    StatsItem.this.printAtHour();
                } catch (Throwable var2) {
                }

            }
        }, Math.abs(UtilAll.computNextHourTimeMillis() - System.currentTimeMillis()), 3600000L, TimeUnit.MILLISECONDS);
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    StatsItem.this.printAtDay();
                } catch (Throwable var2) {
                }

            }
        }, Math.abs(UtilAll.computNextMorningTimeMillis() - System.currentTimeMillis()) - 2000L, 86400000L, TimeUnit.MILLISECONDS);
    }

    public void samplingInSeconds() {
        synchronized(this.csListMinute) {
            this.csListMinute.add(new CallSnapshot(System.currentTimeMillis(), this.times.get(), this.value.get()));
            if (this.csListMinute.size() > 7) {
                this.csListMinute.removeFirst();
            }

        }
    }

    public void samplingInMinutes() {
        synchronized(this.csListHour) {
            this.csListHour.add(new CallSnapshot(System.currentTimeMillis(), this.times.get(), this.value.get()));
            if (this.csListHour.size() > 7) {
                this.csListHour.removeFirst();
            }

        }
    }

    public void samplingInHour() {
        synchronized(this.csListDay) {
            this.csListDay.add(new CallSnapshot(System.currentTimeMillis(), this.times.get(), this.value.get()));
            if (this.csListDay.size() > 25) {
                this.csListDay.removeFirst();
            }

        }
    }

    public void printAtMinutes() {
        StatsSnapshot ss = computeStatsData(this.csListMinute);
        this.log.info(String.format("[%s] [%s] Stats In One Minute, SUM: %d TPS: %.2f AVGPT: %.2f", this.statsName, this.statsKey, ss.getSum(), ss.getTps(), ss.getAvgpt()));
    }

    public void printAtHour() {
        StatsSnapshot ss = computeStatsData(this.csListHour);
        this.log.info(String.format("[%s] [%s] Stats In One Hour, SUM: %d TPS: %.2f AVGPT: %.2f", this.statsName, this.statsKey, ss.getSum(), ss.getTps(), ss.getAvgpt()));
    }

    public void printAtDay() {
        StatsSnapshot ss = computeStatsData(this.csListDay);
        this.log.info(String.format("[%s] [%s] Stats In One Day, SUM: %d TPS: %.2f AVGPT: %.2f", this.statsName, this.statsKey, ss.getSum(), ss.getTps(), ss.getAvgpt()));
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

    public AtomicLong getTimes() {
        return this.times;
    }
}

