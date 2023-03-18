package org.apache.rocketmq.sdk.shade.common.consistenthash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashRouter<T extends Node> {
    private final SortedMap<Long, VirtualNode<T>> ring;
    private final HashFunction hashFunction;

    public ConsistentHashRouter(Collection<T> pNodes, int vNodeCount) {
        this(pNodes, vNodeCount, new MD5Hash());
    }

    public ConsistentHashRouter(Collection<T> pNodes, int vNodeCount, HashFunction hashFunction) {
        this.ring = new TreeMap();
        if (hashFunction == null) {
            throw new NullPointerException("Hash Function is null");
        }
        this.hashFunction = hashFunction;
        if (pNodes != null) {
            for (T pNode : pNodes) {
                addNode(pNode, vNodeCount);
            }
        }
    }

    public void addNode(T pNode, int vNodeCount) {
        if (vNodeCount < 0) {
            throw new IllegalArgumentException("illegal virtual node counts :" + vNodeCount);
        }
        int existingReplicas = getExistingReplicas(pNode);
        for (int i = 0; i < vNodeCount; i++) {
            VirtualNode<T> vNode = new VirtualNode<>(pNode, i + existingReplicas);
            this.ring.put(Long.valueOf(this.hashFunction.hash(vNode.getKey())), vNode);
        }
    }

    public void removeNode(T pNode) {
        Iterator<Long> it = this.ring.keySet().iterator();
        while (it.hasNext()) {
            if (this.ring.get(it.next()).isVirtualNodeOf(pNode)) {
                it.remove();
            }
        }
    }

    public T routeNode(String objectKey) {
        if (this.ring.isEmpty()) {
            return null;
        }
        SortedMap<Long, VirtualNode<T>> tailMap = this.ring.tailMap(Long.valueOf(this.hashFunction.hash(objectKey)));
        return this.ring.get(!tailMap.isEmpty() ? tailMap.firstKey() : this.ring.firstKey()).getPhysicalNode();
    }

    public int getExistingReplicas(T pNode) {
        int replicas = 0;
        for (VirtualNode<T> vNode : this.ring.values()) {
            if (vNode.isVirtualNodeOf(pNode)) {
                replicas++;
            }
        }
        return replicas;
    }

    private static class MD5Hash implements HashFunction {
        MessageDigest instance;

        public MD5Hash() {
            try {
                this.instance = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
            }
        }


        @Override
        public long hash(String key) {
            this.instance.reset();
            this.instance.update(key.getBytes());
            byte[] digest = this.instance.digest();

            long h = 0L;
            for (int i = 0; i < 4; i++) {
                h <<= 8L;
                h |= (digest[i] & 0xFF);
            }
            return h;
        }
    }
}
