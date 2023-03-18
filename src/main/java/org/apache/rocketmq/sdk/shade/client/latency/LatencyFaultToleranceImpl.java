package org.apache.rocketmq.sdk.shade.client.latency;

import org.apache.rocketmq.sdk.shade.client.common.ThreadLocalIndex;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class LatencyFaultToleranceImpl implements LatencyFaultTolerance<String> {
    private final ConcurrentHashMap<String, FaultItem> faultItemTable = new ConcurrentHashMap<>(16);
    private final ThreadLocalIndex whichItemWorst = new ThreadLocalIndex();

    public void updateFaultItem(String name, long currentLatency, long notAvailableDuration) {
        FaultItem old = this.faultItemTable.get(name);
        if (null == old) {
            FaultItem faultItem = new FaultItem(name);
            faultItem.setCurrentLatency(currentLatency);
            faultItem.setStartTimestamp(System.currentTimeMillis() + notAvailableDuration);
            FaultItem old2 = this.faultItemTable.putIfAbsent(name, faultItem);
            if (old2 != null) {
                old2.setCurrentLatency(currentLatency);
                old2.setStartTimestamp(System.currentTimeMillis() + notAvailableDuration);
                return;
            }
            return;
        }
        old.setCurrentLatency(currentLatency);
        old.setStartTimestamp(System.currentTimeMillis() + notAvailableDuration);
    }

    public boolean isAvailable(String name) {
        FaultItem faultItem = this.faultItemTable.get(name);
        if (faultItem != null) {
            return faultItem.isAvailable();
        }
        return true;
    }

    public void remove(String name) {
        this.faultItemTable.remove(name);
    }

    @Override
    public String pickOneAtLeast() {
        Enumeration<FaultItem> elements = this.faultItemTable.elements();
        List<FaultItem> tmpList = new LinkedList<>();
        while (elements.hasMoreElements()) {
            tmpList.add(elements.nextElement());
        }
        if (tmpList.isEmpty()) {
            return null;
        }
        Collections.shuffle(tmpList);
        Collections.sort(tmpList);
        int half = tmpList.size() / 2;
        if (half <= 0) {
            return tmpList.get(0).getName();
        }
        return tmpList.get(this.whichItemWorst.getAndIncrement() % half).getName();
    }

    public String toString() {
        return "LatencyFaultToleranceImpl{faultItemTable=" + this.faultItemTable + ", whichItemWorst=" + this.whichItemWorst + '}';
    }

    public class FaultItem implements Comparable<FaultItem> {
        private final String name;
        private volatile long currentLatency;
        private volatile long startTimestamp;

        public FaultItem(String name) {
            this.name = name;
        }

        public int compareTo(FaultItem other) {
            if (isAvailable() != other.isAvailable()) {
                if (isAvailable()) {
                    return -1;
                }
                if (other.isAvailable()) {
                    return 1;
                }
            }
            if (this.currentLatency < other.currentLatency) {
                return -1;
            }
            if (this.currentLatency > other.currentLatency) {
                return 1;
            }
            if (this.startTimestamp < other.startTimestamp) {
                return -1;
            }
            if (this.startTimestamp > other.startTimestamp) {
                return 1;
            }
            return 0;
        }

        public boolean isAvailable() {
            return System.currentTimeMillis() - this.startTimestamp >= 0;
        }

        @Override
        public int hashCode() {
            return (31 * ((31 * (getName() != null ? getName().hashCode() : 0)) + ((int) (getCurrentLatency() ^ (getCurrentLatency() >>> 32))))) + ((int) (getStartTimestamp() ^ (getStartTimestamp() >>> 32)));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FaultItem)) {
                return false;
            }
            FaultItem faultItem = (FaultItem) o;
            if (getCurrentLatency() == faultItem.getCurrentLatency() && getStartTimestamp() == faultItem.getStartTimestamp()) {
                return getName() != null ? getName().equals(faultItem.getName()) : faultItem.getName() == null;
            }
            return false;
        }

        @Override
        public String toString() {
            return "FaultItem{name='" + this.name + "', currentLatency=" + this.currentLatency + ", startTimestamp=" + this.startTimestamp + '}';
        }

        public String getName() {
            return this.name;
        }

        public long getCurrentLatency() {
            return this.currentLatency;
        }

        public void setCurrentLatency(long currentLatency) {
            this.currentLatency = currentLatency;
        }

        public long getStartTimestamp() {
            return this.startTimestamp;
        }

        public void setStartTimestamp(long startTimestamp) {
            this.startTimestamp = startTimestamp;
        }
    }
}
