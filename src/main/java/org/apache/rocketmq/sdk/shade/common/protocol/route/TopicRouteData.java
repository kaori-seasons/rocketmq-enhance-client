package org.apache.rocketmq.sdk.shade.common.protocol.route;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.PropertyAccessor;

public class TopicRouteData extends RemotingSerializable {
    private String orderTopicConf;
    private List<QueueData> queueDatas;
    private List<BrokerData> brokerDatas;
    private HashMap<String, List<String>> filterServerTable;

    public TopicRouteData cloneTopicRouteData() {
        TopicRouteData topicRouteData = new TopicRouteData();
        topicRouteData.setQueueDatas(new ArrayList());
        topicRouteData.setBrokerDatas(new ArrayList());
        topicRouteData.setFilterServerTable(new HashMap<String, List<String>>());
        topicRouteData.setOrderTopicConf(this.orderTopicConf);
        if (this.queueDatas != null) {
            topicRouteData.getQueueDatas().addAll(this.queueDatas);
        }
        if (this.brokerDatas != null) {
            topicRouteData.getBrokerDatas().addAll(this.brokerDatas);
        }
        if (this.filterServerTable != null) {
            topicRouteData.getFilterServerTable().putAll(this.filterServerTable);
        }
        return topicRouteData;
    }

    public List<QueueData> getQueueDatas() {
        return this.queueDatas;
    }

    public void setQueueDatas(List<QueueData> queueDatas) {
        this.queueDatas = queueDatas;
    }

    public List<BrokerData> getBrokerDatas() {
        return this.brokerDatas;
    }

    public void setBrokerDatas(List<BrokerData> brokerDatas) {
        this.brokerDatas = brokerDatas;
    }

    public HashMap<String, List<String>> getFilterServerTable() {
        return this.filterServerTable;
    }

    public void setFilterServerTable(HashMap<String, List<String>> filterServerTable) {
        this.filterServerTable = filterServerTable;
    }

    public String getOrderTopicConf() {
        return this.orderTopicConf;
    }

    public void setOrderTopicConf(String orderTopicConf) {
        this.orderTopicConf = orderTopicConf;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * 1) + (this.brokerDatas == null ? 0 : this.brokerDatas.hashCode()))) + (this.orderTopicConf == null ? 0 : this.orderTopicConf.hashCode()))) + (this.queueDatas == null ? 0 : this.queueDatas.hashCode()))) + (this.filterServerTable == null ? 0 : this.filterServerTable.hashCode());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TopicRouteData other = (TopicRouteData) obj;
        if (this.brokerDatas == null) {
            if (other.brokerDatas != null) {
                return false;
            }
        } else if (!this.brokerDatas.equals(other.brokerDatas)) {
            return false;
        }
        if (this.orderTopicConf == null) {
            if (other.orderTopicConf != null) {
                return false;
            }
        } else if (!this.orderTopicConf.equals(other.orderTopicConf)) {
            return false;
        }
        if (this.queueDatas == null) {
            if (other.queueDatas != null) {
                return false;
            }
        } else if (!this.queueDatas.equals(other.queueDatas)) {
            return false;
        }
        if (this.filterServerTable == null) {
            if (other.filterServerTable != null) {
                return false;
            }
            return true;
        } else if (!this.filterServerTable.equals(other.filterServerTable)) {
            return false;
        } else {
            return true;
        }
    }

    public String toString() {
        return "TopicRouteData [orderTopicConf=" + this.orderTopicConf + ", queueDatas=" + this.queueDatas + ", brokerDatas=" + this.brokerDatas + ", filterServerTable=" + this.filterServerTable + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
