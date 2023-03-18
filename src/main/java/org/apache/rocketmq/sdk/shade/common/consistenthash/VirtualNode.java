package org.apache.rocketmq.sdk.shade.common.consistenthash;

public class VirtualNode<T extends Node> implements Node {
    final T physicalNode;
    final int replicaIndex;

    public VirtualNode(T physicalNode, int replicaIndex) {
        this.replicaIndex = replicaIndex;
        this.physicalNode = physicalNode;
    }

    @Override
    public String getKey() {
        return this.physicalNode.getKey() + "-" + this.replicaIndex;
    }

    public boolean isVirtualNodeOf(T pNode) {
        return this.physicalNode.getKey().equals(pNode.getKey());
    }

    public T getPhysicalNode() {
        return this.physicalNode;
    }
}
