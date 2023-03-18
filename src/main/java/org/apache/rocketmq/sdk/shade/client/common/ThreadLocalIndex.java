package org.apache.rocketmq.sdk.shade.client.common;

import java.util.Random;

public class ThreadLocalIndex {
    private final ThreadLocal<Integer> threadLocalIndex = new ThreadLocal<>();
    private final Random random = new Random();

    public int getAndIncrement() {
        Integer index = this.threadLocalIndex.get();
        if (null == index) {
            index = Integer.valueOf(Math.abs(this.random.nextInt()));
            if (index.intValue() < 0) {
                index = 0;
            }
            this.threadLocalIndex.set(index);
        }
        Integer index2 = Integer.valueOf(Math.abs(index.intValue() + 1));
        if (index2.intValue() < 0) {
            index2 = 0;
        }
        this.threadLocalIndex.set(index2);
        return index2.intValue();
    }

    public String toString() {
        return "ThreadLocalIndex{threadLocalIndex=" + this.threadLocalIndex.get() + '}';
    }
}
