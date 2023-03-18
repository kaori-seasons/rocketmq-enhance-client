package org.apache.rocketmq.sdk.shade.common.protocol.route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import org.springframework.beans.PropertyAccessor;

public class BrokerData implements Comparable<BrokerData> {
    private String cluster;
    private String brokerName;
    private HashMap<Long, String> brokerAddrs;
    private final Random random = new Random();

    public BrokerData() {
    }

    public BrokerData(String cluster, String brokerName, HashMap<Long, String> brokerAddrs) {
        this.cluster = cluster;
        this.brokerName = brokerName;
        this.brokerAddrs = brokerAddrs;
    }

    public String selectBrokerAddr() {
        String addr = this.brokerAddrs.get(0L);
        if (addr != null) {
            return addr;
        }
        List<String> addrs = new ArrayList<>(this.brokerAddrs.values());
        return addrs.get(this.random.nextInt(addrs.size()));
    }

    public HashMap<Long, String> getBrokerAddrs() {
        return this.brokerAddrs;
    }

    public void setBrokerAddrs(HashMap<Long, String> brokerAddrs) {
        this.brokerAddrs = brokerAddrs;
    }

    public String getCluster() {
        return this.cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    @Override
    public int hashCode() {
        return (31 * ((31 * 1) + (this.brokerAddrs == null ? 0 : this.brokerAddrs.hashCode()))) + (this.brokerName == null ? 0 : this.brokerName.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BrokerData other = (BrokerData) obj;
        if (this.brokerAddrs == null) {
            if (other.brokerAddrs != null) {
                return false;
            }
        } else if (!this.brokerAddrs.equals(other.brokerAddrs)) {
            return false;
        }
        if (this.brokerName == null) {
            if (other.brokerName != null) {
                return false;
            }
            return true;
        } else if (!this.brokerName.equals(other.brokerName)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public String toString() {
        return "BrokerData [brokerName=" + this.brokerName + ", brokerAddrs=" + this.brokerAddrs + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }

    public int compareTo(BrokerData o) {
        return this.brokerName.compareTo(o.getBrokerName());
    }

    public String getBrokerName() {
        return this.brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }
}
