package org.apache.rocketmq.sdk.shade.common.protocol.body;

public class BrokerStatsItem {
    private long sum;
    private double tps;
    private double avgpt;

    public long getSum() {
        return this.sum;
    }

    public void setSum(long sum) {
        this.sum = sum;
    }

    public double getTps() {
        return this.tps;
    }

    public void setTps(double tps) {
        this.tps = tps;
    }

    public double getAvgpt() {
        return this.avgpt;
    }

    public void setAvgpt(double avgpt) {
        this.avgpt = avgpt;
    }
}
