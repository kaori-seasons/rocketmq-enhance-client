package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.ConsumeType;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.SubscriptionData;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

public class ConsumerRunningInfo extends RemotingSerializable {
    public static final String PROP_NAMESERVER_ADDR = "PROP_NAMESERVER_ADDR";
    public static final String PROP_THREADPOOL_CORE_SIZE = "PROP_THREADPOOL_CORE_SIZE";
    public static final String PROP_CONSUME_ORDERLY = "PROP_CONSUMEORDERLY";
    public static final String PROP_CONSUME_TYPE = "PROP_CONSUME_TYPE";
    public static final String PROP_CLIENT_VERSION = "PROP_CLIENT_VERSION";
    public static final String PROP_CONSUMER_START_TIMESTAMP = "PROP_CONSUMER_START_TIMESTAMP";
    private Properties properties = new Properties();
    private TreeSet<SubscriptionData> subscriptionSet = new TreeSet<>();
    private TreeMap<MessageQueue, ProcessQueueInfo> mqTable = new TreeMap<>();
    private TreeMap<String, ConsumeStatus> statusTable = new TreeMap<>();
    private String jstack;

    public static boolean analyzeSubscription(TreeMap<String, ConsumerRunningInfo> criTable) {
        ConsumerRunningInfo prev = criTable.firstEntry().getValue();
        String property = prev.getProperties().getProperty(PROP_CONSUME_TYPE);
        if (property == null) {
            property = ((ConsumeType) prev.getProperties().get(PROP_CONSUME_TYPE)).name();
        }
        boolean push = ConsumeType.valueOf(property) == ConsumeType.CONSUME_PASSIVELY;
        String property2 = prev.getProperties().getProperty(PROP_CONSUMER_START_TIMESTAMP);
        if (property2 == null) {
            property2 = String.valueOf(prev.getProperties().get(PROP_CONSUMER_START_TIMESTAMP));
        }
        boolean startForAWhile = System.currentTimeMillis() - Long.parseLong(property2) > 120000;
        if (!push || !startForAWhile) {
            return true;
        }
        for (Map.Entry<String, ConsumerRunningInfo> next : criTable.entrySet()) {
            if (!next.getValue().getSubscriptionSet().equals(prev.getSubscriptionSet())) {
                return false;
            }
            prev = next.getValue();
        }
        if (prev == null || !prev.getSubscriptionSet().isEmpty()) {
            return true;
        }
        return false;
    }

    public static boolean analyzeRebalance(TreeMap<String, ConsumerRunningInfo> criTable) {
        return true;
    }

    public static String analyzeProcessQueue(String clientId, ConsumerRunningInfo info) {
        StringBuilder sb = new StringBuilder();
        String property = info.getProperties().getProperty(PROP_CONSUME_TYPE);
        if (property == null) {
            property = ((ConsumeType) info.getProperties().get(PROP_CONSUME_TYPE)).name();
        }
        boolean push = ConsumeType.valueOf(property) == ConsumeType.CONSUME_PASSIVELY;
        boolean orderMsg = Boolean.parseBoolean(info.getProperties().getProperty(PROP_CONSUME_ORDERLY));
        if (push) {
            for (Map.Entry<MessageQueue, ProcessQueueInfo> next : info.getMqTable().entrySet()) {
                MessageQueue mq = next.getKey();
                ProcessQueueInfo pq = next.getValue();
                if (!orderMsg) {
                    long diff = System.currentTimeMillis() - pq.getLastConsumeTimestamp();
                    if (diff > 60000 && pq.getCachedMsgCount() > 0) {
                        sb.append(String.format("%s %s can't consume for a while, maybe blocked, %dms%n", clientId, mq, Long.valueOf(diff)));
                    }
                } else if (!pq.isLocked()) {
                    sb.append(String.format("%s %s can't lock for a while, %dms%n", clientId, mq, Long.valueOf(System.currentTimeMillis() - pq.getLastLockTimestamp())));
                } else if (pq.isDroped() && pq.getTryUnlockTimes() > 0) {
                    sb.append(String.format("%s %s unlock %d times, still failed%n", clientId, mq, Long.valueOf(pq.getTryUnlockTimes())));
                }
            }
        }
        return sb.toString();
    }

    public Properties getProperties() {
        return this.properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public TreeSet<SubscriptionData> getSubscriptionSet() {
        return this.subscriptionSet;
    }

    public void setSubscriptionSet(TreeSet<SubscriptionData> subscriptionSet) {
        this.subscriptionSet = subscriptionSet;
    }

    public TreeMap<MessageQueue, ProcessQueueInfo> getMqTable() {
        return this.mqTable;
    }

    public void setMqTable(TreeMap<MessageQueue, ProcessQueueInfo> mqTable) {
        this.mqTable = mqTable;
    }

    public TreeMap<String, ConsumeStatus> getStatusTable() {
        return this.statusTable;
    }

    public void setStatusTable(TreeMap<String, ConsumeStatus> statusTable) {
        this.statusTable = statusTable;
    }

    public String formatString() {
        StringBuilder sb = new StringBuilder();
        sb.append("#Consumer Properties#\n");
        for (Map.Entry<Object, Object> next : this.properties.entrySet()) {
            sb.append(String.format("%-40s: %s%n", next.getKey().toString(), next.getValue().toString()));
        }
        sb.append("\n\n#Consumer Subscription#\n");
        Iterator<SubscriptionData> it = this.subscriptionSet.iterator();
        int i = 0;
        while (it.hasNext()) {
            SubscriptionData next2 = it.next();
            i++;
            sb.append(String.format("%03d Topic: %-40s ClassFilter: %-8s SubExpression: %s%n", Integer.valueOf(i), next2.getTopic(), Boolean.valueOf(next2.isClassFilterMode()), next2.getSubString()));
        }
        sb.append("\n\n#Consumer Offset#\n");
        sb.append(String.format("%-32s  %-32s  %-4s  %-20s%n", "#Topic", "#Broker Name", "#QID", "#Consumer Offset"));
        for (Map.Entry<MessageQueue, ProcessQueueInfo> next3 : this.mqTable.entrySet()) {
            sb.append(String.format("%-32s  %-32s  %-4d  %-20d%n", next3.getKey().getTopic(), next3.getKey().getBrokerName(), Integer.valueOf(next3.getKey().getQueueId()), Long.valueOf(next3.getValue().getCommitOffset())));
        }
        sb.append("\n\n#Consumer MQ Detail#\n");
        sb.append(String.format("%-32s  %-32s  %-4s  %-20s%n", "#Topic", "#Broker Name", "#QID", "#ProcessQueueInfo"));
        for (Map.Entry<MessageQueue, ProcessQueueInfo> next4 : this.mqTable.entrySet()) {
            sb.append(String.format("%-32s  %-32s  %-4d  %s%n", next4.getKey().getTopic(), next4.getKey().getBrokerName(), Integer.valueOf(next4.getKey().getQueueId()), next4.getValue().toString()));
        }
        sb.append("\n\n#Consumer RT&TPS#\n");
        sb.append(String.format("%-32s  %14s %14s %14s %14s %18s %25s%n", "#Topic", "#Pull RT", "#Pull TPS", "#Consume RT", "#ConsumeOK TPS", "#ConsumeFailed TPS", "#ConsumeFailedMsgsInHour"));
        for (Map.Entry<String, ConsumeStatus> next5 : this.statusTable.entrySet()) {
            sb.append(String.format("%-32s  %14.2f %14.2f %14.2f %14.2f %18.2f %25d%n", next5.getKey(), Double.valueOf(next5.getValue().getPullRT()), Double.valueOf(next5.getValue().getPullTPS()), Double.valueOf(next5.getValue().getConsumeRT()), Double.valueOf(next5.getValue().getConsumeOKTPS()), Double.valueOf(next5.getValue().getConsumeFailedTPS()), Long.valueOf(next5.getValue().getConsumeFailedMsgs())));
        }
        if (this.jstack != null) {
            sb.append("\n\n#Consumer jstack#\n");
            sb.append(this.jstack);
        }
        return sb.toString();
    }

    public String getJstack() {
        return this.jstack;
    }

    public void setJstack(String jstack) {
        this.jstack = jstack;
    }
}
