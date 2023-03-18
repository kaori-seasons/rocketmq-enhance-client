package org.apache.rocketmq.sdk.shade.client.impl;

import org.apache.rocketmq.sdk.api.Constants;
import org.apache.rocketmq.sdk.shade.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.sdk.shade.client.impl.producer.MQProducerInner;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.message.MessageConst;
import org.apache.rocketmq.sdk.shade.common.message.MessageDecoder;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.NamespaceUtil;
import org.apache.rocketmq.sdk.shade.common.protocol.RequestCode;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ConsumeMessageDirectlyResult;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.sdk.shade.common.protocol.body.GetConsumerStatusBody;
import org.apache.rocketmq.sdk.shade.common.protocol.body.ResetOffsetBody;
import org.apache.rocketmq.sdk.shade.common.protocol.header.CheckTransactionStateRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.ConsumeMessageDirectlyResultRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetConsumerRunningInfoRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.GetConsumerStatusRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.NotifyConsumerIdsChangedRequestHeader;
import org.apache.rocketmq.sdk.shade.common.protocol.header.ResetOffsetRequestHeader;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingHelper;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;
import org.apache.rocketmq.sdk.shade.remoting.netty.NettyRequestProcessor;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class ClientRemotingProcessor implements NettyRequestProcessor {
    private final InternalLogger log = ClientLogger.getLog();
    private final MQClientInstance mqClientFactory;

    public ClientRemotingProcessor(MQClientInstance mqClientFactory) {
        this.mqClientFactory = mqClientFactory;
    }

    @Override 
    public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws RemotingCommandException {
        switch (request.getCode()) {
            case RequestCode.CHECK_TRANSACTION_STATE :
                return checkTransactionState(ctx, request);
            case 40:
                return notifyConsumerIdsChanged(ctx, request);
            case RequestCode.RESET_CONSUMER_CLIENT_OFFSET:
                return resetOffset(ctx, request);
            case RequestCode.GET_CONSUMER_STATUS_FROM_CLIENT:
                return getConsumeStatus(ctx, request);
            case RequestCode.GET_CONSUMER_RUNNING_INFO:
                return getConsumerRunningInfo(ctx, request);
            case RequestCode.CONSUME_MESSAGE_DIRECTLY :
                return consumeMessageDirectly(ctx, request);
            default:
                return null;
        }
    }

    @Override 
    public boolean rejectRequest() {
        return false;
    }

    public RemotingCommand checkTransactionState(ChannelHandlerContext ctx, RemotingCommand request) throws RemotingCommandException {
        CheckTransactionStateRequestHeader requestHeader = (CheckTransactionStateRequestHeader) request.decodeCommandCustomHeader(CheckTransactionStateRequestHeader.class);
        MessageExt messageExt = MessageDecoder.decode(ByteBuffer.wrap(request.getBody()));
        if (messageExt != null) {
            if (StringUtils.isNotEmpty(this.mqClientFactory.getClientConfig().getNamespace())) {
                messageExt.setTopic(NamespaceUtil.withoutNamespace(messageExt.getTopic(), this.mqClientFactory.getClientConfig().getNamespace()));
            }
            String transactionId = messageExt.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX);
            if (null != transactionId && !"".equals(transactionId)) {
                messageExt.setTransactionId(transactionId);
            }
            String group = messageExt.getProperty(MessageConst.PROPERTY_PRODUCER_GROUP);
            if (group != null) {
                MQProducerInner producer = this.mqClientFactory.selectProducer(group);
                if (producer != null) {
                    producer.checkTransactionState(request.getExtFields().get(Constants.BROKER_ADDRESS), messageExt, requestHeader);
                    return null;
                }
                this.log.debug("checkTransactionState, pick producer by group[{}] failed", group);
                return null;
            }
            this.log.warn("checkTransactionState, pick producer group failed");
            return null;
        }
        this.log.warn("checkTransactionState, decode message failed");
        return null;
    }

    public RemotingCommand notifyConsumerIdsChanged(ChannelHandlerContext ctx, RemotingCommand request) throws RemotingCommandException {
        try {
            this.log.info("receive broker's notification[{}], the consumer group: {} changed, rebalance immediately", RemotingHelper.parseChannelRemoteAddr(ctx.channel()), ((NotifyConsumerIdsChangedRequestHeader) request.decodeCommandCustomHeader(NotifyConsumerIdsChangedRequestHeader.class)).getConsumerGroup());
            this.mqClientFactory.rebalanceImmediately();
            return null;
        } catch (Exception e) {
            this.log.error("notifyConsumerIdsChanged exception", RemotingHelper.exceptionSimpleDesc(e));
            return null;
        }
    }

    public RemotingCommand resetOffset(ChannelHandlerContext ctx, RemotingCommand request) throws RemotingCommandException {
        ResetOffsetRequestHeader requestHeader = (ResetOffsetRequestHeader) request.decodeCommandCustomHeader(ResetOffsetRequestHeader.class);
        this.log.info("invoke reset offset operation from broker. brokerAddr={}, topic={}, group={}, timestamp={}", RemotingHelper.parseChannelRemoteAddr(ctx.channel()), requestHeader.getTopic(), requestHeader.getGroup(), Long.valueOf(requestHeader.getTimestamp()));
        Map<MessageQueue, Long> offsetTable = new HashMap<>();
        if (request.getBody() != null) {
            offsetTable = ((ResetOffsetBody) ResetOffsetBody.decode(request.getBody(), ResetOffsetBody.class)).getOffsetTable();
        }
        this.mqClientFactory.resetOffset(requestHeader.getTopic(), requestHeader.getGroup(), offsetTable);
        return null;
    }

    @Deprecated
    public RemotingCommand getConsumeStatus(ChannelHandlerContext ctx, RemotingCommand request) throws RemotingCommandException {
        RemotingCommand response = RemotingCommand.createResponseCommand(null);
        GetConsumerStatusRequestHeader requestHeader = (GetConsumerStatusRequestHeader) request.decodeCommandCustomHeader(GetConsumerStatusRequestHeader.class);
        Map<MessageQueue, Long> offsetTable = this.mqClientFactory.getConsumerStatus(requestHeader.getTopic(), requestHeader.getGroup());
        GetConsumerStatusBody body = new GetConsumerStatusBody();
        body.setMessageQueueTable(offsetTable);
        response.setBody(body.encode());
        response.setCode(0);
        return response;
    }

    private RemotingCommand getConsumerRunningInfo(ChannelHandlerContext ctx, RemotingCommand request) throws RemotingCommandException {
        RemotingCommand response = RemotingCommand.createResponseCommand(null);
        GetConsumerRunningInfoRequestHeader requestHeader = (GetConsumerRunningInfoRequestHeader) request.decodeCommandCustomHeader(GetConsumerRunningInfoRequestHeader.class);
        ConsumerRunningInfo consumerRunningInfo = this.mqClientFactory.consumerRunningInfo(requestHeader.getConsumerGroup());
        if (null != consumerRunningInfo) {
            if (requestHeader.isJstackEnable()) {
                consumerRunningInfo.setJstack(UtilAll.jstack(Thread.getAllStackTraces()));
            }
            response.setCode(0);
            response.setBody(consumerRunningInfo.encode());
        } else {
            response.setCode(1);
            response.setRemark(String.format("The Consumer Group <%s> not exist in this consumer", requestHeader.getConsumerGroup()));
        }
        return response;
    }

    private RemotingCommand consumeMessageDirectly(ChannelHandlerContext ctx, RemotingCommand request) throws RemotingCommandException {
        RemotingCommand response = RemotingCommand.createResponseCommand(null);
        ConsumeMessageDirectlyResultRequestHeader requestHeader = (ConsumeMessageDirectlyResultRequestHeader) request.decodeCommandCustomHeader(ConsumeMessageDirectlyResultRequestHeader.class);
        ConsumeMessageDirectlyResult result = this.mqClientFactory.consumeMessageDirectly(MessageDecoder.decode(ByteBuffer.wrap(request.getBody())), requestHeader.getConsumerGroup(), requestHeader.getBrokerName());
        if (null != result) {
            response.setCode(0);
            response.setBody(result.encode());
        } else {
            response.setCode(1);
            response.setRemark(String.format("The Consumer Group <%s> not exist in this consumer", requestHeader.getConsumerGroup()));
        }
        return response;
    }
}
