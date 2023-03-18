package org.apache.rocketmq.sdk.shade.common.queue;

import java.util.LinkedList;
import java.util.Queue;

public class RoundQueue<E> {
    private Queue<E> queue = new LinkedList();
    private int capacity;

    public RoundQueue(int capacity) {
        this.capacity = capacity;
    }

    public boolean put(E e) {
        boolean ok = false;
        if (!this.queue.contains(e)) {
            if (this.queue.size() >= this.capacity) {
                this.queue.poll();
            }
            this.queue.add(e);
            ok = true;
        }
        return ok;
    }
}
