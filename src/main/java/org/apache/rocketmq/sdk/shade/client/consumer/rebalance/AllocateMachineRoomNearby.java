package org.apache.rocketmq.sdk.shade.client.consumer.rebalance;

import org.apache.rocketmq.sdk.shade.client.consumer.AllocateMessageQueueStrategy;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;

public class AllocateMachineRoomNearby implements AllocateMessageQueueStrategy {
    private final InternalLogger log = ClientLogger.getLog();
    private final AllocateMessageQueueStrategy allocateMessageQueueStrategy;
    private final MachineRoomResolver machineRoomResolver;

    public interface MachineRoomResolver {
        String brokerDeployIn(MessageQueue messageQueue);

        String consumerDeployIn(String str);
    }

    public AllocateMachineRoomNearby(AllocateMessageQueueStrategy allocateMessageQueueStrategy, MachineRoomResolver machineRoomResolver) throws NullPointerException {
        if (allocateMessageQueueStrategy == null) {
            throw new NullPointerException("allocateMessageQueueStrategy is null");
        } else if (machineRoomResolver == null) {
            throw new NullPointerException("machineRoomResolver is null");
        } else {
            this.allocateMessageQueueStrategy = allocateMessageQueueStrategy;
            this.machineRoomResolver = machineRoomResolver;
        }
    }

    @Override
    public List<MessageQueue> allocate(String consumerGroup, String currentCID, List<MessageQueue> mqAll, List<String> cidAll) {
        if (currentCID == null || currentCID.length() < 1) {
            throw new IllegalArgumentException("currentCID is empty");
        } else if (mqAll == null || mqAll.isEmpty()) {
            throw new IllegalArgumentException("mqAll is null or mqAll empty");
        } else if (cidAll == null || cidAll.isEmpty()) {
            throw new IllegalArgumentException("cidAll is null or cidAll empty");
        } else {
            List<MessageQueue> result = new ArrayList<>();
            if (!cidAll.contains(currentCID)) {
                this.log.info("[BUG] ConsumerGroup: {} The consumerId: {} not in cidAll: {}", consumerGroup, currentCID, cidAll);
                return result;
            }
            Map<String, List<MessageQueue>> mr2Mq = new TreeMap<>();
            for (MessageQueue mq : mqAll) {
                String brokerMachineRoom = this.machineRoomResolver.brokerDeployIn(mq);
                if (StringUtils.isNoneEmpty(brokerMachineRoom)) {
                    if (mr2Mq.get(brokerMachineRoom) == null) {
                        mr2Mq.put(brokerMachineRoom, new ArrayList<MessageQueue>());
                    }
                    mr2Mq.get(brokerMachineRoom).add(mq);
                } else {
                    throw new IllegalArgumentException("Machine room is null for mq " + mq);
                }
            }
            Map<String, List<String>> mr2c = new TreeMap<>();
            for (String cid : cidAll) {
                String consumerMachineRoom = this.machineRoomResolver.consumerDeployIn(cid);
                if (StringUtils.isNoneEmpty(consumerMachineRoom)) {
                    if (mr2c.get(consumerMachineRoom) == null) {
                        mr2c.put(consumerMachineRoom, new ArrayList<String>());
                    }
                    mr2c.get(consumerMachineRoom).add(cid);
                } else {
                    throw new IllegalArgumentException("Machine room is null for consumer id " + cid);
                }
            }
            List<MessageQueue> allocateResults = new ArrayList<>();
            String currentMachineRoom = this.machineRoomResolver.consumerDeployIn(currentCID);
            List<MessageQueue> mqInThisMachineRoom = mr2Mq.remove(currentMachineRoom);
            List<String> consumerInThisMachineRoom = mr2c.get(currentMachineRoom);
            if (mqInThisMachineRoom != null && !mqInThisMachineRoom.isEmpty()) {
                allocateResults.addAll(this.allocateMessageQueueStrategy.allocate(consumerGroup, currentCID, mqInThisMachineRoom, consumerInThisMachineRoom));
            }
            for (String machineRoom : mr2Mq.keySet()) {
                if (!mr2c.containsKey(machineRoom)) {
                    allocateResults.addAll(this.allocateMessageQueueStrategy.allocate(consumerGroup, currentCID, mr2Mq.get(machineRoom), cidAll));
                }
            }
            return allocateResults;
        }
    }

    @Override
    public String getName() {
        return "MACHINE_ROOM_NEARBY-" + this.allocateMessageQueueStrategy.getName();
    }
}
