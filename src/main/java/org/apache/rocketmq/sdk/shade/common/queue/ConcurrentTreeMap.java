package org.apache.rocketmq.sdk.shade.common.queue;

import org.apache.rocketmq.sdk.shade.common.constant.LoggerName;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentTreeMap<K, V> {
    private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.BROKER_LOGGER_NAME);
    private final ReentrantLock lock = new ReentrantLock(true);
    private TreeMap<K, V> tree;
    private RoundQueue<K> roundQueue;

    public ConcurrentTreeMap(int capacity, Comparator<? super K> comparator) {
        this.tree = new TreeMap<>(comparator);
        this.roundQueue = new RoundQueue<>(capacity);
    }

    public Map.Entry<K, V> pollFirstEntry() {
        this.lock.lock();
        try {
            return this.tree.pollFirstEntry();
        } finally {
            this.lock.unlock();
        }
    }

    public V putIfAbsentAndRetExsit(K key, V value) {
        this.lock.lock();
        try {
            if (this.roundQueue.put(key)) {
                V exsit = this.tree.get(key);
                if (null == exsit) {
                    this.tree.put(key, value);
                    exsit = value;
                }
                log.warn("putIfAbsentAndRetExsit success. " + key);
                this.lock.unlock();
                return exsit;
            }
            V exsit2 = this.tree.get(key);
            this.lock.unlock();
            return exsit2;
        } catch (Throwable th) {
            this.lock.unlock();
            throw th;
        }
    }
}
