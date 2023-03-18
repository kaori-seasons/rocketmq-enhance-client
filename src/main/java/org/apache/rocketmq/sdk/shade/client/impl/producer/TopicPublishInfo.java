package org.apache.rocketmq.sdk.shade.client.impl.producer;

import org.apache.rocketmq.sdk.shade.client.common.ThreadLocalIndex;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.route.QueueData;
import org.apache.rocketmq.sdk.shade.common.protocol.route.TopicRouteData;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.PropertyAccessor;

public class TopicPublishInfo {
    private boolean orderTopic = false;
    private boolean haveTopicRouterInfo = false;
    private List<MessageQueue> messageQueueList = new ArrayList();
    private volatile ThreadLocalIndex sendWhichQueue = new ThreadLocalIndex();
    private TopicRouteData topicRouteData;

    public boolean isOrderTopic() {
        return this.orderTopic;
    }

    public void setOrderTopic(boolean orderTopic) {
        this.orderTopic = orderTopic;
    }

    public boolean ok() {
        return null != this.messageQueueList && !this.messageQueueList.isEmpty();
    }

    public List<MessageQueue> getMessageQueueList() {
        return this.messageQueueList;
    }

    public void setMessageQueueList(List<MessageQueue> messageQueueList) {
        this.messageQueueList = messageQueueList;
    }

    public ThreadLocalIndex getSendWhichQueue() {
        return this.sendWhichQueue;
    }

    public void setSendWhichQueue(ThreadLocalIndex sendWhichQueue) {
        this.sendWhichQueue = sendWhichQueue;
    }

    public boolean isHaveTopicRouterInfo() {
        return this.haveTopicRouterInfo;
    }

    public void setHaveTopicRouterInfo(boolean haveTopicRouterInfo) {
        this.haveTopicRouterInfo = haveTopicRouterInfo;
    }

    public MessageQueue selectOneMessageQueue(String lastBrokerName) {
        if (lastBrokerName == null) {
            return selectOneMessageQueue();
        }
        int index = this.sendWhichQueue.getAndIncrement();
        for (int i = 0; i < this.messageQueueList.size(); i++) {
            index++;
            int pos = Math.abs(index) % this.messageQueueList.size();
            if (pos < 0) {
                pos = 0;
            }
            MessageQueue mq = this.messageQueueList.get(pos);
            if (!mq.getBrokerName().equals(lastBrokerName)) {
                return mq;
            }
        }
        return selectOneMessageQueue();
    }

    public MessageQueue selectOneMessageQueue() {
        int pos = Math.abs(this.sendWhichQueue.getAndIncrement()) % this.messageQueueList.size();
        if (pos < 0) {
            pos = 0;
        }
        return this.messageQueueList.get(pos);
    }

    public int getQueueIdByBroker(String brokerName) {
        for (int i = 0; i < this.topicRouteData.getQueueDatas().size(); i++) {
            QueueData queueData = this.topicRouteData.getQueueDatas().get(i);
            if (queueData.getBrokerName().equals(brokerName)) {
                return queueData.getWriteQueueNums();
            }
        }
        return -1;
    }

    public String toString() {
        return "TopicPublishInfo [orderTopic=" + this.orderTopic + ", messageQueueList=" + this.messageQueueList + ", sendWhichQueue=" + this.sendWhichQueue + ", haveTopicRouterInfo=" + this.haveTopicRouterInfo + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }

    public TopicRouteData getTopicRouteData() {
        return this.topicRouteData;
    }

    public void setTopicRouteData(TopicRouteData topicRouteData) {
        this.topicRouteData = topicRouteData;
    }
}
