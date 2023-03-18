package org.apache.rocketmq.sdk.shade.client.impl.consumer;

import org.apache.rocketmq.sdk.shade.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.common.message.MessageAccessor;
import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ProcessQueueInfo;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ProcessQueue {
    public static final long REBALANCE_LOCK_MAX_LIVE_TIME = Long.parseLong(System.getProperty("rocketmq.client.rebalance.lockMaxLiveTime", "30000"));
    public static final long REBALANCE_LOCK_INTERVAL = Long.parseLong(System.getProperty("rocketmq.client.rebalance.lockInterval", "20000"));
    private static final long PULL_MAX_IDLE_TIME = Long.parseLong(System.getProperty("rocketmq.client.pull.pullMaxIdleTime", "120000"));
    private final InternalLogger log = ClientLogger.getLog();
    private final ReadWriteLock lockTreeMap = new ReentrantReadWriteLock();
    private final TreeMap<Long, MessageExt> msgTreeMap = new TreeMap();
    private final AtomicLong msgCount = new AtomicLong();
    private final AtomicLong msgSize = new AtomicLong();
    private final Lock lockConsume = new ReentrantLock();
    private final TreeMap<Long, MessageExt> consumingMsgOrderlyTreeMap = new TreeMap();
    private final AtomicLong tryUnlockTimes = new AtomicLong(0L);
    private volatile long queueOffsetMax = 0L;
    private volatile boolean dropped = false;
    private volatile long lastPullTimestamp = System.currentTimeMillis();
    private volatile long lastConsumeTimestamp = System.currentTimeMillis();
    private volatile boolean locked = false;
    private volatile long lastLockTimestamp = System.currentTimeMillis();
    private volatile boolean consuming = false;
    private volatile long msgAccCnt = 0L;

    public ProcessQueue() {
    }

    public boolean isLockExpired() {
        return System.currentTimeMillis() - this.lastLockTimestamp > REBALANCE_LOCK_MAX_LIVE_TIME;
    }

    public boolean isPullExpired() {
        return System.currentTimeMillis() - this.lastPullTimestamp > PULL_MAX_IDLE_TIME;
    }

    public void cleanExpiredMsg(DefaultMQPushConsumer pushConsumer) {
        if (!pushConsumer.getDefaultMQPushConsumerImpl().isConsumeOrderly()) {
            int loop = this.msgTreeMap.size() < 16 ? this.msgTreeMap.size() : 16;

            for(int i = 0; i < loop; ++i) {
                MessageExt msg = null;

                try {
                    this.lockTreeMap.readLock().lockInterruptibly();

                    try {
                        if (this.msgTreeMap.isEmpty() || System.currentTimeMillis() - Long.parseLong(MessageAccessor.getConsumeStartTimeStamp((Message)this.msgTreeMap.firstEntry().getValue())) <= pushConsumer.getConsumeTimeout() * 60L * 1000L) {
                            break;
                        }

                        msg = (MessageExt)this.msgTreeMap.firstEntry().getValue();
                    } finally {
                        this.lockTreeMap.readLock().unlock();
                    }
                } catch (InterruptedException var24) {
                    this.log.error("getExpiredMsg exception", var24);
                }

                try {
                    pushConsumer.sendMessageBack(msg, 3);
                    this.log.info("send expire msg back. topic={}, msgId={}, storeHost={}, queueId={}, queueOffset={}", new Object[]{msg.getTopic(), msg.getMsgId(), msg.getStoreHost(), msg.getQueueId(), msg.getQueueOffset()});

                    try {
                        this.lockTreeMap.writeLock().lockInterruptibly();

                        try {
                            if (!this.msgTreeMap.isEmpty() && msg.getQueueOffset() == (Long)this.msgTreeMap.firstKey()) {
                                try {
                                    this.removeMessage(Collections.singletonList(msg));
                                } catch (Exception var19) {
                                    this.log.error("send expired msg exception", var19);
                                }
                            }
                        } finally {
                            this.lockTreeMap.writeLock().unlock();
                        }
                    } catch (InterruptedException var21) {
                        this.log.error("getExpiredMsg exception", var21);
                    }
                } catch (Exception var22) {
                    this.log.error("send expired msg exception", var22);
                }
            }

        }
    }

    public boolean putMessage(List<MessageExt> msgs) {
        boolean dispatchToConsume = false;

        try {
            this.lockTreeMap.writeLock().lockInterruptibly();

            try {
                int validMsgCnt = 0;
                Iterator var4 = msgs.iterator();

                while(var4.hasNext()) {
                    MessageExt msg = (MessageExt)var4.next();
                    MessageExt old = (MessageExt)this.msgTreeMap.put(msg.getQueueOffset(), msg);
                    if (null == old) {
                        ++validMsgCnt;
                        this.queueOffsetMax = msg.getQueueOffset();
                        this.msgSize.addAndGet((long)msg.getBody().length);
                    }
                }

                this.msgCount.addAndGet((long)validMsgCnt);
                if (!this.msgTreeMap.isEmpty() && !this.consuming) {
                    dispatchToConsume = true;
                    this.consuming = true;
                }

                if (!msgs.isEmpty()) {
                    MessageExt messageExt = (MessageExt)msgs.get(msgs.size() - 1);
                    String property = messageExt.getProperty("MAX_OFFSET");
                    if (property != null) {
                        long accTotal = Long.parseLong(property) - messageExt.getQueueOffset();
                        if (accTotal > 0L) {
                            this.msgAccCnt = accTotal;
                        }
                    }
                }
            } finally {
                this.lockTreeMap.writeLock().unlock();
            }
        } catch (InterruptedException var12) {
            this.log.error("putMessage exception", var12);
        }

        return dispatchToConsume;
    }

    public long getMaxSpan() {
        try {
            this.lockTreeMap.readLock().lockInterruptibly();

            long var1;
            try {
                if (this.msgTreeMap.isEmpty()) {
                    return 0L;
                }

                var1 = (Long)this.msgTreeMap.lastKey() - (Long)this.msgTreeMap.firstKey();
            } finally {
                this.lockTreeMap.readLock().unlock();
            }

            return var1;
        } catch (InterruptedException var7) {
            this.log.error("getMaxSpan exception", var7);
            return 0L;
        }
    }

    public long removeMessage(List<MessageExt> msgs) {
        long result = -1L;
        long now = System.currentTimeMillis();

        try {
            this.lockTreeMap.writeLock().lockInterruptibly();
            this.lastConsumeTimestamp = now;

            try {
                if (!this.msgTreeMap.isEmpty()) {
                    result = this.queueOffsetMax + 1L;
                    int removedCnt = 0;
                    Iterator var7 = msgs.iterator();

                    while(var7.hasNext()) {
                        MessageExt msg = (MessageExt)var7.next();
                        MessageExt prev = (MessageExt)this.msgTreeMap.remove(msg.getQueueOffset());
                        if (prev != null) {
                            --removedCnt;
                            this.msgSize.addAndGet((long)(0 - msg.getBody().length));
                        }
                    }

                    this.msgCount.addAndGet((long)removedCnt);
                    if (!this.msgTreeMap.isEmpty()) {
                        result = (Long)this.msgTreeMap.firstKey();
                    }
                }
            } finally {
                this.lockTreeMap.writeLock().unlock();
            }
        } catch (Throwable var14) {
            this.log.error("removeMessage exception", var14);
        }

        return result;
    }

    public TreeMap<Long, MessageExt> getMsgTreeMap() {
        return this.msgTreeMap;
    }

    public AtomicLong getMsgCount() {
        return this.msgCount;
    }

    public AtomicLong getMsgSize() {
        return this.msgSize;
    }

    public boolean isDropped() {
        return this.dropped;
    }

    public void setDropped(boolean dropped) {
        this.dropped = dropped;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void rollback() {
        try {
            this.lockTreeMap.writeLock().lockInterruptibly();

            try {
                this.msgTreeMap.putAll(this.consumingMsgOrderlyTreeMap);
                this.consumingMsgOrderlyTreeMap.clear();
            } finally {
                this.lockTreeMap.writeLock().unlock();
            }
        } catch (InterruptedException var5) {
            this.log.error("rollback exception", var5);
        }

    }

    public long commit() {
        try {
            this.lockTreeMap.writeLock().lockInterruptibly();

            try {
                Long offset = (Long)this.consumingMsgOrderlyTreeMap.lastKey();
                this.msgCount.addAndGet((long)(0 - this.consumingMsgOrderlyTreeMap.size()));
                Iterator var2 = this.consumingMsgOrderlyTreeMap.values().iterator();

                while(var2.hasNext()) {
                    MessageExt msg = (MessageExt)var2.next();
                    this.msgSize.addAndGet((long)(0 - msg.getBody().length));
                }

                this.consumingMsgOrderlyTreeMap.clear();
                if (offset == null) {
                    return -1L;
                } else {
                    long var9 = offset + 1L;
                    return var9;
                }
            } finally {
                this.lockTreeMap.writeLock().unlock();
            }
        } catch (InterruptedException var8) {
            this.log.error("commit exception", var8);
            return -1L;
        }
    }

    public void makeMessageToCosumeAgain(List<MessageExt> msgs) {
        try {
            this.lockTreeMap.writeLock().lockInterruptibly();

            try {
                Iterator var2 = msgs.iterator();

                while(var2.hasNext()) {
                    MessageExt msg = (MessageExt)var2.next();
                    this.consumingMsgOrderlyTreeMap.remove(msg.getQueueOffset());
                    this.msgTreeMap.put(msg.getQueueOffset(), msg);
                }
            } finally {
                this.lockTreeMap.writeLock().unlock();
            }
        } catch (InterruptedException var8) {
            this.log.error("makeMessageToCosumeAgain exception", var8);
        }

    }

    public List<MessageExt> takeMessags(int batchSize) {
        List<MessageExt> result = new ArrayList(batchSize);
        long now = System.currentTimeMillis();

        try {
            this.lockTreeMap.writeLock().lockInterruptibly();
            this.lastConsumeTimestamp = now;

            try {
                if (!this.msgTreeMap.isEmpty()) {
                    for(int i = 0; i < batchSize; ++i) {
                        Map.Entry<Long, MessageExt> entry = this.msgTreeMap.pollFirstEntry();
                        if (entry == null) {
                            break;
                        }

                        result.add(entry.getValue());
                        this.consumingMsgOrderlyTreeMap.put(entry.getKey(), entry.getValue());
                    }
                }

                if (result.isEmpty()) {
                    this.consuming = false;
                }
            } finally {
                this.lockTreeMap.writeLock().unlock();
            }
        } catch (InterruptedException var11) {
            this.log.error("take Messages exception", var11);
        }

        return result;
    }

    public boolean hasTempMessage() {
        try {
            this.lockTreeMap.readLock().lockInterruptibly();

            boolean var1;
            try {
                var1 = !this.msgTreeMap.isEmpty();
            } finally {
                this.lockTreeMap.readLock().unlock();
            }

            return var1;
        } catch (InterruptedException var6) {
            return true;
        }
    }

    public void clear() {
        try {
            this.lockTreeMap.writeLock().lockInterruptibly();

            try {
                this.msgTreeMap.clear();
                this.consumingMsgOrderlyTreeMap.clear();
                this.msgCount.set(0L);
                this.msgSize.set(0L);
                this.queueOffsetMax = 0L;
            } finally {
                this.lockTreeMap.writeLock().unlock();
            }
        } catch (InterruptedException var5) {
            this.log.error("rollback exception", var5);
        }

    }

    public long getLastLockTimestamp() {
        return this.lastLockTimestamp;
    }

    public void setLastLockTimestamp(long lastLockTimestamp) {
        this.lastLockTimestamp = lastLockTimestamp;
    }

    public Lock getLockConsume() {
        return this.lockConsume;
    }

    public long getLastPullTimestamp() {
        return this.lastPullTimestamp;
    }

    public void setLastPullTimestamp(long lastPullTimestamp) {
        this.lastPullTimestamp = lastPullTimestamp;
    }

    public long getMsgAccCnt() {
        return this.msgAccCnt;
    }

    public void setMsgAccCnt(long msgAccCnt) {
        this.msgAccCnt = msgAccCnt;
    }

    public long getTryUnlockTimes() {
        return this.tryUnlockTimes.get();
    }

    public void incTryUnlockTimes() {
        this.tryUnlockTimes.incrementAndGet();
    }

    public void fillProcessQueueInfo(ProcessQueueInfo info) {
        try {
            this.lockTreeMap.readLock().lockInterruptibly();
            if (!this.msgTreeMap.isEmpty()) {
                info.setCachedMsgMinOffset((Long)this.msgTreeMap.firstKey());
                info.setCachedMsgMaxOffset((Long)this.msgTreeMap.lastKey());
                info.setCachedMsgCount(this.msgTreeMap.size());
                info.setCachedMsgSizeInMiB((int)(this.msgSize.get() / 1048576L));
            }

            if (!this.consumingMsgOrderlyTreeMap.isEmpty()) {
                info.setTransactionMsgMinOffset((Long)this.consumingMsgOrderlyTreeMap.firstKey());
                info.setTransactionMsgMaxOffset((Long)this.consumingMsgOrderlyTreeMap.lastKey());
                info.setTransactionMsgCount(this.consumingMsgOrderlyTreeMap.size());
            }

            info.setLocked(this.locked);
            info.setTryUnlockTimes(this.tryUnlockTimes.get());
            info.setLastLockTimestamp(this.lastLockTimestamp);
            info.setDroped(this.dropped);
            info.setLastPullTimestamp(this.lastPullTimestamp);
            info.setLastConsumeTimestamp(this.lastConsumeTimestamp);
        } catch (Exception var6) {
        } finally {
            this.lockTreeMap.readLock().unlock();
        }

    }

    public long getLastConsumeTimestamp() {
        return this.lastConsumeTimestamp;
    }

    public void setLastConsumeTimestamp(long lastConsumeTimestamp) {
        this.lastConsumeTimestamp = lastConsumeTimestamp;
    }
}
