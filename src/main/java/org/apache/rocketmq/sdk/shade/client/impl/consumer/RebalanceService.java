package org.apache.rocketmq.sdk.shade.client.impl.consumer;

import org.apache.rocketmq.sdk.shade.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.common.ServiceThread;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

public class RebalanceService extends ServiceThread {
    private static long waitInterval = Long.parseLong(System.getProperty("rocketmq.client.rebalance.waitInterval", "20000"));
    private final InternalLogger log = ClientLogger.getLog();
    private final MQClientInstance mqClientFactory;

    public RebalanceService(MQClientInstance mqClientFactory) {
        this.mqClientFactory = mqClientFactory;
    }

    @Override
    public void run() {
        this.log.info(getServiceName() + " service started");
        while (!isStopped()) {
            waitForRunning(waitInterval);
            this.mqClientFactory.doRebalance();
        }
        this.log.info(getServiceName() + " service end");
    }

    @Override
    public String getServiceName() {
        return RebalanceService.class.getSimpleName();
    }
}
