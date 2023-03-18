package org.apache.rocketmq.sdk.shade.client.consumer;

public class PullTaskContext {
    private int pullNextDelayTimeMillis = 200;
    private MQPullConsumer pullConsumer;

    public int getPullNextDelayTimeMillis() {
        return this.pullNextDelayTimeMillis;
    }

    public void setPullNextDelayTimeMillis(int pullNextDelayTimeMillis) {
        this.pullNextDelayTimeMillis = pullNextDelayTimeMillis;
    }

    public MQPullConsumer getPullConsumer() {
        return this.pullConsumer;
    }

    public void setPullConsumer(MQPullConsumer pullConsumer) {
        this.pullConsumer = pullConsumer;
    }
}
