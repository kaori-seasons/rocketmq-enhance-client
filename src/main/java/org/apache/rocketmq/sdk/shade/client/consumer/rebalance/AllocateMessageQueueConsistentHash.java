package org.apache.rocketmq.sdk.shade.client.consumer.rebalance;

import org.apache.rocketmq.sdk.shade.client.consumer.AllocateMessageQueueStrategy;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.common.consistenthash.ConsistentHashRouter;
import org.apache.rocketmq.sdk.shade.common.consistenthash.HashFunction;
import org.apache.rocketmq.sdk.shade.common.consistenthash.Node;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AllocateMessageQueueConsistentHash implements AllocateMessageQueueStrategy {
    private final InternalLogger log;
    private final int virtualNodeCnt;
    private final HashFunction customHashFunction;

    public AllocateMessageQueueConsistentHash() {
        this(10);
    }

    public AllocateMessageQueueConsistentHash(int virtualNodeCnt) {
        this(virtualNodeCnt, null);
    }

    public AllocateMessageQueueConsistentHash(int virtualNodeCnt, HashFunction customHashFunction) {
        this.log = ClientLogger.getLog();
        if (virtualNodeCnt < 0) {
            throw new IllegalArgumentException("illegal virtualNodeCnt :" + virtualNodeCnt);
        }
        this.virtualNodeCnt = virtualNodeCnt;
        this.customHashFunction = customHashFunction;
    }

    @Override
    public List<MessageQueue> allocate(String consumerGroup, String currentCID, List<MessageQueue> mqAll, List<String> cidAll) {
        ConsistentHashRouter<ClientNode> router;
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
            Collection<ClientNode> cidNodes = new ArrayList<>();
            for (String cid : cidAll) {
                cidNodes.add(new ClientNode(cid));
            }
            if (this.customHashFunction != null) {
                router = new ConsistentHashRouter<>(cidNodes, this.virtualNodeCnt, this.customHashFunction);
            } else {
                router = new ConsistentHashRouter<>(cidNodes, this.virtualNodeCnt);
            }
            List<MessageQueue> results = new ArrayList<>();
            for (MessageQueue mq : mqAll) {
                ClientNode clientNode = router.routeNode(mq.toString());
                if (clientNode != null && currentCID.equals(clientNode.getKey())) {
                    results.add(mq);
                }
            }
            return results;
        }
    }

    @Override
    public String getName() {
        return "CONSISTENT_HASH";
    }

    private static class ClientNode implements Node {
        private final String clientID;

        public ClientNode(String clientID) {
            this.clientID = clientID;
        }

        @Override
        public String getKey() {
            return this.clientID;
        }
    }
}
