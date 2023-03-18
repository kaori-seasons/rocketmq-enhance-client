package org.apache.rocketmq.sdk.api.impl.tracehook;

import org.apache.rocketmq.sdk.shade.client.consumer.listener.ConsumeReturnType;
import org.apache.rocketmq.sdk.shade.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.sdk.shade.client.hook.ConsumeMessageHook;
import org.apache.rocketmq.sdk.shade.common.MixAll;
import org.apache.rocketmq.sdk.shade.common.message.MessageConst;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.common.protocol.NamespaceUtil;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceBean;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceContext;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceType;
import org.apache.rocketmq.sdk.trace.core.dispatch.AsyncDispatcher;

import java.util.ArrayList;
import java.util.List;

public class RMQClientConsumeMessageHookImpl implements ConsumeMessageHook {
    private AsyncDispatcher localDispatcher;

    public RMQClientConsumeMessageHookImpl(AsyncDispatcher localDispatcher) {
        this.localDispatcher = localDispatcher;
    }

    @Override 
    public String hookName() {
        return "TuxeClientConsumeMessageHook";
    }

    @Override 
    public void consumeMessageBefore(ConsumeMessageContext context) {
        if (!(context == null || context.getMsgList() == null || context.getMsgList().isEmpty())) {
            RMQTraceContext tuxeContext = new RMQTraceContext();
            context.setMqTraceContext(tuxeContext);
            tuxeContext.setTraceType(RMQTraceType.SubBefore);
            tuxeContext.setGroupName(NamespaceUtil.withoutNamespace(context.getConsumerGroup(), context.getNamespace()));
            List<RMQTraceBean> beans = new ArrayList<>();
            for (MessageExt msg : context.getMsgList()) {
                if (msg != null) {
                    String regionId = msg.getProperty(MessageConst.PROPERTY_MSG_REGION);
                    String traceOn = msg.getProperty(MessageConst.PROPERTY_TRACE_SWITCH);
                    if (traceOn == null || !traceOn.equals("false")) {
                        RMQTraceBean traceBean = new RMQTraceBean();
                        traceBean.setTopic(NamespaceUtil.withoutNamespace(msg.getTopic(), context.getNamespace()));
                        traceBean.setMsgId(msg.getMsgId());
                        traceBean.setTags(msg.getTags());
                        traceBean.setKeys(msg.getKeys());
                        traceBean.setStoreTime(msg.getStoreTimestamp());
                        traceBean.setBodyLength(msg.getStoreSize());
                        traceBean.setRetryTimes(msg.getReconsumeTimes());
                        tuxeContext.setRegionId(regionId);
                        beans.add(traceBean);
                    }
                }
            }
            if (beans.size() > 0) {
                tuxeContext.setTraceBeans(beans);
                tuxeContext.setTimeStamp(System.currentTimeMillis());
                this.localDispatcher.append(tuxeContext);
            }
        }
    }

    @Override 
    public void consumeMessageAfter(ConsumeMessageContext context) {
        if (context != null && context.getMsgList() != null && !context.getMsgList().isEmpty()) {
            RMQTraceContext subBeforeContext = (RMQTraceContext) context.getMqTraceContext();
            if (subBeforeContext.getTraceBeans() != null && subBeforeContext.getTraceBeans().size() >= 1) {
                RMQTraceContext subAfterContext = new RMQTraceContext();
                subAfterContext.setTraceType(RMQTraceType.SubAfter);
                subAfterContext.setRegionId(subBeforeContext.getRegionId());
                subAfterContext.setGroupName(subBeforeContext.getGroupName());
                subAfterContext.setRequestId(subBeforeContext.getRequestId());
                subAfterContext.setSuccess(context.isSuccess());
                subAfterContext.setCostTime((int) ((System.currentTimeMillis() - subBeforeContext.getTimeStamp()) / ((long) context.getMsgList().size())));
                subAfterContext.setTraceBeans(subBeforeContext.getTraceBeans());
                String contextType = context.getProps().get(MixAll.CONSUME_CONTEXT_TYPE);
                if (contextType != null) {
                    subAfterContext.setContextCode(ConsumeReturnType.valueOf(contextType).ordinal());
                }
                this.localDispatcher.append(subAfterContext);
            }
        }
    }
}
