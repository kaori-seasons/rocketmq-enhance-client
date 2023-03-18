package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

public class BrokerStatsData extends RemotingSerializable {
    private BrokerStatsItem statsMinute;
    private BrokerStatsItem statsHour;
    private BrokerStatsItem statsDay;

    public BrokerStatsItem getStatsMinute() {
        return this.statsMinute;
    }

    public void setStatsMinute(BrokerStatsItem statsMinute) {
        this.statsMinute = statsMinute;
    }

    public BrokerStatsItem getStatsHour() {
        return this.statsHour;
    }

    public void setStatsHour(BrokerStatsItem statsHour) {
        this.statsHour = statsHour;
    }

    public BrokerStatsItem getStatsDay() {
        return this.statsDay;
    }

    public void setStatsDay(BrokerStatsItem statsDay) {
        this.statsDay = statsDay;
    }
}
