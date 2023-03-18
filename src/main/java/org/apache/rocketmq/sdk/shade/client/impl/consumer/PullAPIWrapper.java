package org.apache.rocketmq.sdk.shade.client.impl.consumer;

import org.apache.rocketmq.sdk.shade.client.consumer.PullCallback;
import org.apache.rocketmq.sdk.shade.client.consumer.PullResult;
import org.apache.rocketmq.sdk.shade.client.consumer.PullStatus;
import org.apache.rocketmq.sdk.shade.client.exception.MQBrokerException;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.hook.FilterMessageContext;
import org.apache.rocketmq.sdk.shade.client.hook.FilterMessageHook;
import org.apache.rocketmq.sdk.shade.client.impl.CommunicationMode;
import org.apache.rocketmq.sdk.shade.client.impl.FindBrokerResult;
import org.apache.rocketmq.sdk.shade.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.common.MQVersion;
import org.apache.rocketmq.sdk.shade.common.filter.ExpressionType;
import org.apache.rocketmq.sdk.shade.common.message.MessageAccessor;
import org.apache.rocketmq.sdk.shade.common.message.MessageConst;
import org.apache.rocketmq.sdk.shade.common.message.MessageDecoder;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.header.PullMessageRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.heartbeat.SubscriptionData;
import org.apache.rocketmq.sdk.shade.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.sdk.shade.common.sysflag.PullSysFlag;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class PullAPIWrapper {
    private final MQClientInstance mQClientFactory;
    private final String consumerGroup;
    private final boolean unitMode;
    private final InternalLogger log = ClientLogger.getLog();
    private ConcurrentMap<MessageQueue, AtomicLong> pullFromWhichNodeTable = new ConcurrentHashMap(32);
    private volatile boolean connectBrokerByUser = false;
    private volatile long defaultBrokerId = 0;
    private Random random = new Random(System.currentTimeMillis());
    private ArrayList<FilterMessageHook> filterMessageHookList = new ArrayList<>();

    public PullAPIWrapper(MQClientInstance mQClientFactory, String consumerGroup, boolean unitMode) {
        this.mQClientFactory = mQClientFactory;
        this.consumerGroup = consumerGroup;
        this.unitMode = unitMode;
    }

    public PullResult processPullResult(MessageQueue mq, PullResult pullResult, SubscriptionData subscriptionData) {
        PullResultExt pullResultExt = (PullResultExt) pullResult;
        updatePullFromWhichNode(mq, pullResultExt.getSuggestWhichBrokerId());
        if (PullStatus.FOUND == pullResult.getPullStatus()) {
            List<MessageExt> msgList = MessageDecoder.decodesBatch(ByteBuffer.wrap(pullResultExt.getMessageBinary()), true, true, true);
            List<MessageExt> msgListFilterAgain = msgList;
            if (!subscriptionData.getTagsSet().isEmpty() && !subscriptionData.isClassFilterMode()) {
                msgListFilterAgain = new ArrayList<>(msgList.size());
                for (MessageExt msg : msgList) {
                    if (msg.getTags() != null && subscriptionData.getTagsSet().contains(msg.getTags())) {
                        msgListFilterAgain.add(msg);
                    }
                }
            }
            if (hasHook()) {
                FilterMessageContext filterMessageContext = new FilterMessageContext();
                filterMessageContext.setUnitMode(this.unitMode);
                filterMessageContext.setMsgList(msgListFilterAgain);
                executeHook(filterMessageContext);
            }
            for (MessageExt msg2 : msgListFilterAgain) {
                String traFlag = msg2.getProperty(MessageConst.PROPERTY_TRANSACTION_PREPARED);
                if (traFlag != null && Boolean.parseBoolean(traFlag)) {
                    msg2.setTransactionId(msg2.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX));
                }
                MessageAccessor.putProperty(msg2, MessageConst.PROPERTY_MIN_OFFSET, Long.toString(pullResult.getMinOffset()));
                MessageAccessor.putProperty(msg2, MessageConst.PROPERTY_MAX_OFFSET, Long.toString(pullResult.getMaxOffset()));
                msg2.setBrokerName(mq.getBrokerName());
            }
            pullResultExt.setMsgFoundList(msgListFilterAgain);
        }
        pullResultExt.setMessageBinary(null);
        return pullResult;
    }

    public void updatePullFromWhichNode(MessageQueue mq, long brokerId) {
        AtomicLong suggest = this.pullFromWhichNodeTable.get(mq);
        if (null == suggest) {
            this.pullFromWhichNodeTable.put(mq, new AtomicLong(brokerId));
        } else {
            suggest.set(brokerId);
        }
    }

    public boolean hasHook() {
        return !this.filterMessageHookList.isEmpty();
    }

    public void executeHook(FilterMessageContext context) {
        if (!this.filterMessageHookList.isEmpty()) {
            Iterator<FilterMessageHook> it = this.filterMessageHookList.iterator();
            while (it.hasNext()) {
                FilterMessageHook hook = it.next();
                try {
                    hook.filterMessage(context);
                } catch (Throwable th) {
                    this.log.error("execute hook error. hookName={}", hook.hookName());
                }
            }
        }
    }

    public PullResult pullKernelImpl(MessageQueue mq, String subExpression, String expressionType, long subVersion, long offset, int maxNums, int sysFlag, long commitOffset, long brokerSuspendMaxTimeMillis, long timeoutMillis, CommunicationMode communicationMode, PullCallback pullCallback) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        FindBrokerResult findBrokerResult = this.mQClientFactory.findBrokerAddressInSubscribe(mq.getBrokerName(), recalculatePullFromWhichNode(mq), false);
        if (null == findBrokerResult) {
            this.mQClientFactory.updateTopicRouteInfoFromNameServer(mq.getTopic());
            findBrokerResult = this.mQClientFactory.findBrokerAddressInSubscribe(mq.getBrokerName(), recalculatePullFromWhichNode(mq), false);
        }
        if (findBrokerResult == null) {
            throw new MQClientException("The broker[" + mq.getBrokerName() + "] not exist", (Throwable) null);
        } else if (ExpressionType.isTagType(expressionType) || findBrokerResult.getBrokerVersion() >= MQVersion.Version.V4_1_0_SNAPSHOT.ordinal()) {
            int sysFlagInner = sysFlag;
            if (findBrokerResult.isSlave()) {
                sysFlagInner = PullSysFlag.clearCommitOffsetFlag(sysFlagInner);
            }
            PullMessageRequestHeader requestHeader = new PullMessageRequestHeader();
            requestHeader.setConsumerGroup(this.consumerGroup);
            requestHeader.setTopic(mq.getTopic());
            requestHeader.setQueueId(Integer.valueOf(mq.getQueueId()));
            requestHeader.setQueueOffset(Long.valueOf(offset));
            requestHeader.setMaxMsgNums(Integer.valueOf(maxNums));
            requestHeader.setSysFlag(Integer.valueOf(sysFlagInner));
            requestHeader.setCommitOffset(Long.valueOf(commitOffset));
            requestHeader.setSuspendTimeoutMillis(Long.valueOf(brokerSuspendMaxTimeMillis));
            requestHeader.setSubscription(subExpression);
            requestHeader.setSubVersion(Long.valueOf(subVersion));
            requestHeader.setExpressionType(expressionType);
            String brokerAddr = findBrokerResult.getBrokerAddr();
            if (PullSysFlag.hasClassFilterFlag(sysFlagInner)) {
                brokerAddr = computPullFromWhichFilterServer(mq.getTopic(), brokerAddr);
            }
            return this.mQClientFactory.getMQClientAPIImpl().pullMessage(brokerAddr, requestHeader, timeoutMillis, communicationMode, pullCallback);
        } else {
            throw new MQClientException("The broker[" + mq.getBrokerName() + ", " + findBrokerResult.getBrokerVersion() + "] does not upgrade to support for filter message by " + expressionType, (Throwable) null);
        }
    }

    public PullResult pullKernelImpl(MessageQueue mq, String subExpression, long subVersion, long offset, int maxNums, int sysFlag, long commitOffset, long brokerSuspendMaxTimeMillis, long timeoutMillis, CommunicationMode communicationMode, PullCallback pullCallback) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return pullKernelImpl(mq, subExpression, ExpressionType.TAG, subVersion, offset, maxNums, sysFlag, commitOffset, brokerSuspendMaxTimeMillis, timeoutMillis, communicationMode, pullCallback);
    }

    public long recalculatePullFromWhichNode(MessageQueue mq) {
        if (isConnectBrokerByUser()) {
            return this.defaultBrokerId;
        }
        AtomicLong suggest = this.pullFromWhichNodeTable.get(mq);
        if (suggest != null) {
            return suggest.get();
        }
        return 0;
    }

    private String computPullFromWhichFilterServer(String topic, String brokerAddr) throws MQClientException {
        List<String> list;
        ConcurrentMap<String, TopicRouteData> topicRouteTable = this.mQClientFactory.getTopicRouteTable();
        if (topicRouteTable != null && (list = topicRouteTable.get(topic).getFilterServerTable().get(brokerAddr)) != null && !list.isEmpty()) {
            return list.get(randomNum() % list.size());
        }
        throw new MQClientException("Find Filter Server Failed, Broker Addr: " + brokerAddr + " topic: " + topic, (Throwable) null);
    }

    public boolean isConnectBrokerByUser() {
        return this.connectBrokerByUser;
    }

    public void setConnectBrokerByUser(boolean connectBrokerByUser) {
        this.connectBrokerByUser = connectBrokerByUser;
    }

    public int randomNum() {
        int value = this.random.nextInt();
        if (value < 0) {
            value = Math.abs(value);
            if (value < 0) {
                value = 0;
            }
        }
        return value;
    }

    public void registerFilterMessageHook(ArrayList<FilterMessageHook> filterMessageHookList) {
        this.filterMessageHookList = filterMessageHookList;
    }

    public long getDefaultBrokerId() {
        return this.defaultBrokerId;
    }

    public void setDefaultBrokerId(long defaultBrokerId) {
        this.defaultBrokerId = defaultBrokerId;
    }
}
