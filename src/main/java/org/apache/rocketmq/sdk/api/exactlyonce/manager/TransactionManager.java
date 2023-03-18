package org.apache.rocketmq.sdk.api.exactlyonce.manager;

import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.DataSourceConfig;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.core.MQTxConnection;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.util.LogUtil;
import org.apache.rocketmq.sdk.api.impl.util.ClientLoggerUtil;
import org.apache.rocketmq.sdk.shade.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import io.netty.util.internal.ConcurrentSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.StringUtils;

public class TransactionManager {
    private static final InternalLogger LOGGER = ClientLoggerUtil.getClientLogger();
    private static ConcurrentMap<String, ConcurrentSet<DefaultMQPushConsumer>> consumerMap = new ConcurrentHashMap();
    private static ConcurrentMap<DataSourceConfig, ConcurrentSet<String>> consumerSessionMap = new ConcurrentHashMap();
    private static volatile AtomicBoolean started = new AtomicBoolean(false);
    private static TxRecordManager txRecordManager;

    public static void start(Properties properties) {
        if (started.compareAndSet(false, true)) {
            LogUtil.info(LOGGER, "start TransactionManager...");
            txRecordManager = new TxRecordManager(properties);
            txRecordManager.start();
            MetricService.getInstance().start();
        }
    }

    public static void stop() {
        if (started.compareAndSet(true, false)) {
            txRecordManager.stop();
            MetricService.getInstance().stop();
        }
    }

    public static void addConsumer(String consumerGroup, DefaultMQPushConsumer defaultMQPushConsumer) {
        ConcurrentSet<DefaultMQPushConsumer> consumerSet = consumerMap.get(consumerGroup);
        if (consumerSet == null) {
            ConcurrentSet<DefaultMQPushConsumer> newConsumerSet = new ConcurrentSet<>();
            newConsumerSet.add(defaultMQPushConsumer);
            ConcurrentSet<DefaultMQPushConsumer> old = consumerMap.putIfAbsent(consumerGroup, newConsumerSet);
            if (old != null) {
                old.add(defaultMQPushConsumer);
                return;
            }
            return;
        }
        consumerSet.add(defaultMQPushConsumer);
    }

    public static Set<DefaultMQPushConsumer> getConsumer(String consumerGroup) {
        return consumerMap.get(consumerGroup);
    }

    public static void flushTxRecord(MQTxConnection connection, MQTxContext context) throws Exception {
        txRecordManager.flushTxRecord(connection, context);
    }

    public static void updateConsumeSessionMap(DataSourceConfig config, String consumerGroup) {
        if (config != null && !StringUtils.isEmpty(consumerGroup)) {
            ConcurrentSet<String> cidSet = consumerSessionMap.get(config);
            if (cidSet == null) {
                ConcurrentSet<String> newCidSet = new ConcurrentSet<>();
                newCidSet.add(consumerGroup);
                ConcurrentSet<String> old = consumerSessionMap.putIfAbsent(config, newCidSet);
                if (old != null) {
                    old.add(consumerGroup);
                    return;
                }
                return;
            }
            cidSet.add(consumerGroup);
        }
    }

    public static ConcurrentMap<DataSourceConfig, ConcurrentSet<String>> getConsumerSessionMap() {
        return consumerSessionMap;
    }
}
