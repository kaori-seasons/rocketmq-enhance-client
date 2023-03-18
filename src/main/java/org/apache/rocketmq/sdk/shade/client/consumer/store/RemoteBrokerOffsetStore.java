package org.apache.rocketmq.sdk.shade.client.consumer.store;

import org.apache.rocketmq.sdk.shade.client.exception.MQBrokerException;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.impl.FindBrokerResult;
import org.apache.rocketmq.sdk.shade.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.common.MixAll;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.header.QueryConsumerOffsetRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.UpdateConsumerOffsetRequestHeader;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.util.backoff.FixedBackOff;

public class RemoteBrokerOffsetStore implements OffsetStore {
    private static final InternalLogger log = ClientLogger.getLog();
    private final MQClientInstance mQClientFactory;
    private final String groupName;
    private ConcurrentMap<MessageQueue, AtomicLong> offsetTable = new ConcurrentHashMap();

    public RemoteBrokerOffsetStore(MQClientInstance mQClientFactory, String groupName) {
        this.mQClientFactory = mQClientFactory;
        this.groupName = groupName;
    }

    @Override
    public void load() {
    }

    @Override
    public void updateOffset(MessageQueue mq, long offset, boolean increaseOnly) {
        if (mq != null) {
            AtomicLong offsetOld = this.offsetTable.get(mq);
            if (null == offsetOld) {
                offsetOld = this.offsetTable.putIfAbsent(mq, new AtomicLong(offset));
            }
            if (null == offsetOld) {
                return;
            }
            if (increaseOnly) {
                MixAll.compareAndIncreaseOnly(offsetOld, offset);
            } else {
                offsetOld.set(offset);
            }
        }
    }

    @Override
    public long readOffset(MessageQueue mq, ReadOffsetType type) {
        if (mq == null) {
            return -1;
        }
        switch (type) {
            case MEMORY_FIRST_THEN_STORE:
            case READ_FROM_MEMORY:
                AtomicLong offset = this.offsetTable.get(mq);
                if (offset != null) {
                    return offset.get();
                }
                if (ReadOffsetType.READ_FROM_MEMORY == type) {
                    return -1;
                }
                return -1;
            case READ_FROM_STORE:
                try {
                    long brokerOffset = fetchConsumeOffsetFromBroker(mq);
                    updateOffset(mq, new AtomicLong(brokerOffset).get(), false);
                    return brokerOffset;
                } catch (MQBrokerException e) {
                    return -1;
                } catch (Exception e2) {
                    log.warn("fetchConsumeOffsetFromBroker exception, " + mq, (Throwable) e2);
                    return -2;
                }
            default:
                return -1;
        }
    }

    @Override
    public void persistAll(Set<MessageQueue> mqs) {
        if (!(null == mqs || mqs.isEmpty())) {
            HashSet<MessageQueue> unusedMQ = new HashSet<>();
            if (!mqs.isEmpty()) {
                for (Map.Entry<MessageQueue, AtomicLong> entry : this.offsetTable.entrySet()) {
                    MessageQueue mq = entry.getKey();
                    AtomicLong offset = entry.getValue();
                    if (offset != null) {
                        if (mqs.contains(mq)) {
                            try {
                                updateConsumeOffsetToBroker(mq, offset.get());
                                log.info("[persistAll] Group: {} ClientId: {} updateConsumeOffsetToBroker {} {}", this.groupName, this.mQClientFactory.getClientId(), mq, Long.valueOf(offset.get()));
                            } catch (Exception e) {
                                log.error("updateConsumeOffsetToBroker exception, " + mq.toString(), (Throwable) e);
                            }
                        } else {
                            unusedMQ.add(mq);
                        }
                    }
                }
            }
            if (!unusedMQ.isEmpty()) {
                Iterator<MessageQueue> it = unusedMQ.iterator();
                while (it.hasNext()) {
                    MessageQueue mq2 = it.next();
                    this.offsetTable.remove(mq2);
                    log.info("remove unused mq, {}, {}", mq2, this.groupName);
                }
            }
        }
    }

    @Override
    public void persist(MessageQueue mq) {
        AtomicLong offset = this.offsetTable.get(mq);
        if (offset != null) {
            try {
                updateConsumeOffsetToBroker(mq, offset.get());
                log.info("[persist] Group: {} ClientId: {} updateConsumeOffsetToBroker {} {}", this.groupName, this.mQClientFactory.getClientId(), mq, Long.valueOf(offset.get()));
            } catch (Exception e) {
                log.error("updateConsumeOffsetToBroker exception, " + mq.toString(), (Throwable) e);
            }
        }
    }

    @Override
    public void removeOffset(MessageQueue mq) {
        if (mq != null) {
            this.offsetTable.remove(mq);
            log.info("remove unnecessary messageQueue offset. group={}, mq={}, offsetTableSize={}", this.groupName, mq, Integer.valueOf(this.offsetTable.size()));
        }
    }

    @Override
    public Map<MessageQueue, Long> cloneOffsetTable(String topic) {
        Map<MessageQueue, Long> cloneOffsetTable = new HashMap<>();
        for (Map.Entry<MessageQueue, AtomicLong> entry : this.offsetTable.entrySet()) {
            MessageQueue mq = entry.getKey();
            if (UtilAll.isBlank(topic) || topic.equals(mq.getTopic())) {
                cloneOffsetTable.put(mq, Long.valueOf(entry.getValue().get()));
            }
        }
        return cloneOffsetTable;
    }

    private void updateConsumeOffsetToBroker(MessageQueue mq, long offset) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        updateConsumeOffsetToBroker(mq, offset, true);
    }

    @Override
    public void updateConsumeOffsetToBroker(MessageQueue mq, long offset, boolean isOneway) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        FindBrokerResult findBrokerResult = this.mQClientFactory.findBrokerAddressInAdmin(mq.getBrokerName());
        if (null == findBrokerResult) {
            this.mQClientFactory.updateTopicRouteInfoFromNameServer(mq.getTopic());
            findBrokerResult = this.mQClientFactory.findBrokerAddressInAdmin(mq.getBrokerName());
        }
        if (findBrokerResult != null) {
            UpdateConsumerOffsetRequestHeader requestHeader = new UpdateConsumerOffsetRequestHeader();
            requestHeader.setTopic(mq.getTopic());
            requestHeader.setConsumerGroup(this.groupName);
            requestHeader.setQueueId(Integer.valueOf(mq.getQueueId()));
            requestHeader.setCommitOffset(Long.valueOf(offset));
            if (isOneway) {
                this.mQClientFactory.getMQClientAPIImpl().updateConsumerOffsetOneway(findBrokerResult.getBrokerAddr(), requestHeader, FixedBackOff.DEFAULT_INTERVAL);
            } else {
                this.mQClientFactory.getMQClientAPIImpl().updateConsumerOffset(findBrokerResult.getBrokerAddr(), requestHeader, FixedBackOff.DEFAULT_INTERVAL);
            }
        } else {
            throw new MQClientException("The broker[" + mq.getBrokerName() + "] not exist", (Throwable) null);
        }
    }

    private long fetchConsumeOffsetFromBroker(MessageQueue mq) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        FindBrokerResult findBrokerResult = this.mQClientFactory.findBrokerAddressInAdmin(mq.getBrokerName());
        if (null == findBrokerResult) {
            this.mQClientFactory.updateTopicRouteInfoFromNameServer(mq.getTopic());
            findBrokerResult = this.mQClientFactory.findBrokerAddressInAdmin(mq.getBrokerName());
        }
        if (findBrokerResult != null) {
            QueryConsumerOffsetRequestHeader requestHeader = new QueryConsumerOffsetRequestHeader();
            requestHeader.setTopic(mq.getTopic());
            requestHeader.setConsumerGroup(this.groupName);
            requestHeader.setQueueId(Integer.valueOf(mq.getQueueId()));
            return this.mQClientFactory.getMQClientAPIImpl().queryConsumerOffset(findBrokerResult.getBrokerAddr(), requestHeader, FixedBackOff.DEFAULT_INTERVAL);
        }
        throw new MQClientException("The broker[" + mq.getBrokerName() + "] not exist", (Throwable) null);
    }
}
