package org.apache.rocketmq.sdk.shade.client.latency;

import org.apache.rocketmq.sdk.shade.client.impl.producer.TopicPublishInfo;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import io.netty.handler.traffic.AbstractTrafficShapingHandler;
import org.springframework.util.backoff.ExponentialBackOff;

public class MQFaultStrategy {
    private static final InternalLogger log = ClientLogger.getLog();
    private final LatencyFaultTolerance<String> latencyFaultTolerance = new LatencyFaultToleranceImpl();
    private boolean sendLatencyFaultEnable = false;
    private long[] latencyMax = {50, 100, 550, 1000, ExponentialBackOff.DEFAULT_INITIAL_INTERVAL, 3000, AbstractTrafficShapingHandler.DEFAULT_MAX_TIME};
    private long[] notAvailableDuration = {0, 0, ExponentialBackOff.DEFAULT_MAX_INTERVAL, 60000, 120000, 180000, 600000};

    public long[] getNotAvailableDuration() {
        return this.notAvailableDuration;
    }

    public void setNotAvailableDuration(long[] notAvailableDuration) {
        this.notAvailableDuration = notAvailableDuration;
    }

    public long[] getLatencyMax() {
        return this.latencyMax;
    }

    public void setLatencyMax(long[] latencyMax) {
        this.latencyMax = latencyMax;
    }

    public boolean isSendLatencyFaultEnable() {
        return this.sendLatencyFaultEnable;
    }

    public void setSendLatencyFaultEnable(boolean sendLatencyFaultEnable) {
        this.sendLatencyFaultEnable = sendLatencyFaultEnable;
    }

    public MessageQueue selectOneMessageQueue(TopicPublishInfo tpInfo, String lastBrokerName) {
        String notBestBroker = "";
        int writeQueueNums = 0;
        if (!this.sendLatencyFaultEnable) {
            return tpInfo.selectOneMessageQueue(lastBrokerName);
        }
        try {
            int index = tpInfo.getSendWhichQueue().getAndIncrement();
            for (int i = 0; i < tpInfo.getMessageQueueList().size(); i++) {
                index++;
                int pos = Math.abs(index) % tpInfo.getMessageQueueList().size();
                if (pos < 0) {
                    pos = 0;
                }
                MessageQueue mq = tpInfo.getMessageQueueList().get(pos);
                if (this.latencyFaultTolerance.isAvailable(mq.getBrokerName()) && (null == lastBrokerName || mq.getBrokerName().equals(lastBrokerName))) {
                    return mq;
                }
            }
            notBestBroker = this.latencyFaultTolerance.pickOneAtLeast();
            writeQueueNums = tpInfo.getQueueIdByBroker(notBestBroker);
        } catch (Exception e) {
            log.error("Error occurred when selecting message queue", (Throwable) e);
        }
        if (writeQueueNums > 0) {
            MessageQueue mq2 = tpInfo.selectOneMessageQueue();
            if (notBestBroker != null) {
                mq2.setBrokerName(notBestBroker);
                mq2.setQueueId(tpInfo.getSendWhichQueue().getAndIncrement() % writeQueueNums);
            }
            return mq2;
        }
        this.latencyFaultTolerance.remove(notBestBroker);
        return tpInfo.selectOneMessageQueue();
    }

    public void updateFaultItem(String brokerName, long currentLatency, boolean isolation) {
        if (this.sendLatencyFaultEnable) {
            long duration = this.computeNotAvailableDuration(isolation ? 30000L : currentLatency);
            this.latencyFaultTolerance.updateFaultItem(brokerName, currentLatency, duration);
        }

    }

    private long computeNotAvailableDuration(long currentLatency) {
        for (int i = this.latencyMax.length - 1; i >= 0; i--) {
            if (currentLatency >= this.latencyMax[i]) {
                return this.notAvailableDuration[i];
            }
        }
        return 0;
    }
}
