package org.apache.rocketmq.sdk.shade.client.consumer.rebalance;

import org.apache.rocketmq.sdk.shade.client.consumer.AllocateMessageQueueStrategy;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AllocateMessageQueueByMachineRoom implements AllocateMessageQueueStrategy {
    private Set<String> consumeridcs;

    @Override
    public List<MessageQueue> allocate(String consumerGroup, String currentCID, List<MessageQueue> mqAll, List<String> cidAll) {
        ArrayList arrayList = new ArrayList();
        int currentIndex = cidAll.indexOf(currentCID);
        if (currentIndex < 0) {
            return arrayList;
        }
        List<MessageQueue> premqAll = new ArrayList<>();
        for (MessageQueue mq : mqAll) {
            String[] temp = mq.getBrokerName().split("@");
            if (temp.length == 2 && this.consumeridcs.contains(temp[0])) {
                premqAll.add(mq);
            }
        }
        int mod = premqAll.size() / cidAll.size();
        int rem = premqAll.size() % cidAll.size();
        int startIndex = mod * currentIndex;
        int endIndex = startIndex + mod;
        for (int i = startIndex; i < endIndex; i++) {
            arrayList.add(mqAll.get(i));
        }
        if (rem > currentIndex) {
            arrayList.add(premqAll.get(currentIndex + (mod * cidAll.size())));
        }
        return arrayList;
    }

    @Override
    public String getName() {
        return "MACHINE_ROOM";
    }

    public Set<String> getConsumeridcs() {
        return this.consumeridcs;
    }

    public void setConsumeridcs(Set<String> consumeridcs) {
        this.consumeridcs = consumeridcs;
    }
}
