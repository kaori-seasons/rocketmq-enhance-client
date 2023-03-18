package org.apache.rocketmq.sdk.shade.client.stat;

import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ConsumeStatus;
import org.apache.rocketmq.sdk.shade.common.stats.StatsItemSet;
import org.apache.rocketmq.sdk.shade.common.stats.StatsSnapshot;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

import java.util.concurrent.ScheduledExecutorService;

public class ConsumerStatsManager {
    private static final InternalLogger log = ClientLogger.getLog();
    private static final String TOPIC_AND_GROUP_CONSUME_OK_TPS = "CONSUME_OK_TPS";
    private static final String TOPIC_AND_GROUP_CONSUME_FAILED_TPS = "CONSUME_FAILED_TPS";
    private static final String TOPIC_AND_GROUP_CONSUME_RT = "CONSUME_RT";
    private static final String TOPIC_AND_GROUP_PULL_TPS = "PULL_TPS";
    private static final String TOPIC_AND_GROUP_PULL_RT = "PULL_RT";
    private final StatsItemSet topicAndGroupConsumeOKTPS;
    private final StatsItemSet topicAndGroupConsumeRT;
    private final StatsItemSet topicAndGroupConsumeFailedTPS;
    private final StatsItemSet topicAndGroupPullTPS;
    private final StatsItemSet topicAndGroupPullRT;

    public ConsumerStatsManager(ScheduledExecutorService scheduledExecutorService) {
        this.topicAndGroupConsumeOKTPS = new StatsItemSet(TOPIC_AND_GROUP_CONSUME_OK_TPS, scheduledExecutorService, log);
        this.topicAndGroupConsumeRT = new StatsItemSet(TOPIC_AND_GROUP_CONSUME_RT, scheduledExecutorService, log);
        this.topicAndGroupConsumeFailedTPS = new StatsItemSet(TOPIC_AND_GROUP_CONSUME_FAILED_TPS, scheduledExecutorService, log);
        this.topicAndGroupPullTPS = new StatsItemSet(TOPIC_AND_GROUP_PULL_TPS, scheduledExecutorService, log);
        this.topicAndGroupPullRT = new StatsItemSet(TOPIC_AND_GROUP_PULL_RT, scheduledExecutorService, log);
    }

    public void start() {
    }

    public void shutdown() {
    }

    public void incPullRT(String group, String topic, long rt) {
        this.topicAndGroupPullRT.addValue(topic + "@" + group, (int) rt, 1);
    }

    public void incPullTPS(String group, String topic, long msgs) {
        this.topicAndGroupPullTPS.addValue(topic + "@" + group, (int) msgs, 1);
    }

    public void incConsumeRT(String group, String topic, long rt) {
        this.topicAndGroupConsumeRT.addValue(topic + "@" + group, (int) rt, 1);
    }

    public void incConsumeOKTPS(String group, String topic, long msgs) {
        this.topicAndGroupConsumeOKTPS.addValue(topic + "@" + group, (int) msgs, 1);
    }

    public void incConsumeFailedTPS(String group, String topic, long msgs) {
        this.topicAndGroupConsumeFailedTPS.addValue(topic + "@" + group, (int) msgs, 1);
    }

    public ConsumeStatus consumeStatus(String group, String topic) {
        ConsumeStatus cs = new ConsumeStatus();
        StatsSnapshot ss = getPullRT(group, topic);
        if (ss != null) {
            cs.setPullRT(ss.getAvgpt());
        }
        StatsSnapshot ss2 = getPullTPS(group, topic);
        if (ss2 != null) {
            cs.setPullTPS(ss2.getTps());
        }
        StatsSnapshot ss3 = getConsumeRT(group, topic);
        if (ss3 != null) {
            cs.setConsumeRT(ss3.getAvgpt());
        }
        StatsSnapshot ss4 = getConsumeOKTPS(group, topic);
        if (ss4 != null) {
            cs.setConsumeOKTPS(ss4.getTps());
        }
        StatsSnapshot ss5 = getConsumeFailedTPS(group, topic);
        if (ss5 != null) {
            cs.setConsumeFailedTPS(ss5.getTps());
        }
        StatsSnapshot ss6 = this.topicAndGroupConsumeFailedTPS.getStatsDataInHour(topic + "@" + group);
        if (ss6 != null) {
            cs.setConsumeFailedMsgs(ss6.getSum());
        }
        return cs;
    }

    private StatsSnapshot getPullRT(String group, String topic) {
        return this.topicAndGroupPullRT.getStatsDataInMinute(topic + "@" + group);
    }

    private StatsSnapshot getPullTPS(String group, String topic) {
        return this.topicAndGroupPullTPS.getStatsDataInMinute(topic + "@" + group);
    }

    private StatsSnapshot getConsumeRT(String group, String topic) {
        StatsSnapshot statsData = this.topicAndGroupConsumeRT.getStatsDataInMinute(topic + "@" + group);
        if (0 == statsData.getSum()) {
            statsData = this.topicAndGroupConsumeRT.getStatsDataInHour(topic + "@" + group);
        }
        return statsData;
    }

    private StatsSnapshot getConsumeOKTPS(String group, String topic) {
        return this.topicAndGroupConsumeOKTPS.getStatsDataInMinute(topic + "@" + group);
    }

    private StatsSnapshot getConsumeFailedTPS(String group, String topic) {
        return this.topicAndGroupConsumeFailedTPS.getStatsDataInMinute(topic + "@" + group);
    }
}
