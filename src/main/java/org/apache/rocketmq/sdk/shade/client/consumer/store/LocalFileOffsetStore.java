package org.apache.rocketmq.sdk.shade.client.consumer.store;

import org.apache.rocketmq.sdk.shade.client.exception.MQBrokerException;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.common.MixAll;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.help.FAQUrl;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class LocalFileOffsetStore implements OffsetStore {
    public static final String LOCAL_OFFSET_STORE_DIR = System.getProperty("rocketmq.client.localOffsetStoreDir", System.getProperty("user.home") + File.separator + ".rocketmq_offsets");
    private static final InternalLogger log = ClientLogger.getLog();
    private final MQClientInstance mQClientFactory;
    private final String groupName;
    private final String storePath;
    private ConcurrentMap<MessageQueue, AtomicLong> offsetTable = new ConcurrentHashMap();

    public LocalFileOffsetStore(MQClientInstance mQClientFactory, String groupName) {
        this.mQClientFactory = mQClientFactory;
        this.groupName = groupName;
        this.storePath = LOCAL_OFFSET_STORE_DIR + File.separator + this.mQClientFactory.getClientId() + File.separator + this.groupName + File.separator + "offsets.json";
    }

    @Override 
    public void load() throws MQClientException {
        OffsetSerializeWrapper offsetSerializeWrapper = readLocalOffset();
        if (!(offsetSerializeWrapper == null || offsetSerializeWrapper.getOffsetTable() == null)) {
            this.offsetTable.putAll(offsetSerializeWrapper.getOffsetTable());
            for (MessageQueue mq : offsetSerializeWrapper.getOffsetTable().keySet()) {
                log.info("load consumer's offset, {} {} {}", this.groupName, mq, Long.valueOf(offsetSerializeWrapper.getOffsetTable().get(mq).get()));
            }
        }
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
        AtomicLong offset;
        if (mq == null) {
            return -1;
        }
        switch (type) {
            case MEMORY_FIRST_THEN_STORE:
            case READ_FROM_MEMORY:
                AtomicLong offset2 = this.offsetTable.get(mq);
                if (offset2 != null) {
                    return offset2.get();
                }
                if (ReadOffsetType.READ_FROM_MEMORY == type) {
                    return -1;
                }
                return -1;
            case READ_FROM_STORE:
                try {
                    OffsetSerializeWrapper offsetSerializeWrapper = readLocalOffset();
                    if (offsetSerializeWrapper == null || offsetSerializeWrapper.getOffsetTable() == null || (offset = offsetSerializeWrapper.getOffsetTable().get(mq)) == null) {
                        return -1;
                    }
                    updateOffset(mq, offset.get(), false);
                    return offset.get();
                } catch (MQClientException e) {
                    return -1;
                }
            default:
                return -1;
        }
    }

    @Override 
    public void persistAll(Set<MessageQueue> mqs) {
        if (null != mqs && !mqs.isEmpty()) {
            OffsetSerializeWrapper offsetSerializeWrapper = new OffsetSerializeWrapper();
            for (Map.Entry<MessageQueue, AtomicLong> entry : this.offsetTable.entrySet()) {
                if (mqs.contains(entry.getKey())) {
                    offsetSerializeWrapper.getOffsetTable().put(entry.getKey(), entry.getValue());
                }
            }
            String jsonString = offsetSerializeWrapper.toJson(true);
            if (jsonString != null) {
                try {
                    MixAll.string2File(jsonString, this.storePath);
                } catch (IOException e) {
                    log.error("persistAll consumer offset Exception, " + this.storePath, (Throwable) e);
                }
            }
        }
    }

    @Override 
    public void persist(MessageQueue mq) {
    }

    @Override 
    public void removeOffset(MessageQueue mq) {
    }

    @Override 
    public void updateConsumeOffsetToBroker(MessageQueue mq, long offset, boolean isOneway) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
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

    private OffsetSerializeWrapper readLocalOffset() throws MQClientException {
        String content = null;
        try {
            content = MixAll.file2String(this.storePath);
        } catch (IOException e) {
            log.warn("Load local offset store file exception", (Throwable) e);
        }
        if (null == content || content.length() == 0) {
            return readLocalOffsetBak();
        }
        try {
            return (OffsetSerializeWrapper) OffsetSerializeWrapper.fromJson(content, OffsetSerializeWrapper.class);
        } catch (Exception e2) {
            log.warn("readLocalOffset Exception, and try to correct", (Throwable) e2);
            return readLocalOffsetBak();
        }
    }

    private OffsetSerializeWrapper readLocalOffsetBak() throws MQClientException {
        String content = null;
        try {
            content = MixAll.file2String(this.storePath + ".bak");
        } catch (IOException e) {
            log.warn("Load local offset store bak file exception", (Throwable) e);
        }
        if (content == null || content.length() <= 0) {
            return null;
        }
        try {
            return (OffsetSerializeWrapper) OffsetSerializeWrapper.fromJson(content, OffsetSerializeWrapper.class);
        } catch (Exception e2) {
            log.warn("readLocalOffset Exception", (Throwable) e2);
            throw new MQClientException("readLocalOffset Exception, maybe fastjson version too low" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), e2);
        }
    }
}
