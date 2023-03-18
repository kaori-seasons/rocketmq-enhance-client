package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.common.protocol.route.BrokerData;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ClusterInfo extends RemotingSerializable {
    private HashMap<String, BrokerData> brokerAddrTable;
    private HashMap<String, Set<String>> clusterAddrTable;

    public HashMap<String, BrokerData> getBrokerAddrTable() {
        return this.brokerAddrTable;
    }

    public void setBrokerAddrTable(HashMap<String, BrokerData> brokerAddrTable) {
        this.brokerAddrTable = brokerAddrTable;
    }

    public HashMap<String, Set<String>> getClusterAddrTable() {
        return this.clusterAddrTable;
    }

    public void setClusterAddrTable(HashMap<String, Set<String>> clusterAddrTable) {
        this.clusterAddrTable = clusterAddrTable;
    }

    public String[] retrieveAllAddrByCluster(String cluster) {
        List<String> addrs = new ArrayList<>();
        if (this.clusterAddrTable.containsKey(cluster)) {
            for (String brokerName : this.clusterAddrTable.get(cluster)) {
                BrokerData brokerData = this.brokerAddrTable.get(brokerName);
                if (null != brokerData) {
                    addrs.addAll(brokerData.getBrokerAddrs().values());
                }
            }
        }
        return (String[]) addrs.toArray(new String[0]);
    }

    public String[] retrieveAllClusterNames() {
        return (String[]) this.clusterAddrTable.keySet().toArray(new String[0]);
    }
}
