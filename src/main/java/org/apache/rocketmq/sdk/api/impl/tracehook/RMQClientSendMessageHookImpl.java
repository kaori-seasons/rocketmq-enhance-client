package org.apache.rocketmq.sdk.api.impl.tracehook;

import org.apache.rocketmq.sdk.shade.client.hook.SendMessageContext;
import org.apache.rocketmq.sdk.shade.client.hook.SendMessageHook;
import org.apache.rocketmq.sdk.shade.client.producer.SendStatus;
import org.apache.rocketmq.sdk.shade.common.MixAll;
import org.apache.rocketmq.sdk.shade.common.protocol.NamespaceUtil;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceBean;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceContext;
import org.apache.rocketmq.sdk.trace.core.common.RMQTraceType;
import org.apache.rocketmq.sdk.trace.core.dispatch.AsyncDispatcher;

import java.util.ArrayList;

public class RMQClientSendMessageHookImpl implements SendMessageHook {
    private AsyncDispatcher localDispatcher;

    public RMQClientSendMessageHookImpl(AsyncDispatcher localDispatcher) {
        this.localDispatcher = localDispatcher;
    }

    @Override
    public String hookName() {
        return "TuxeClientSendMessageHook";
    }

    @Override
    public void sendMessageBefore(SendMessageContext context) {
        if (context != null && !context.getMessage().getTopic().startsWith(MixAll.SYSTEM_TOPIC_PREFIX)) {
            RMQTraceContext tuxeContext = new RMQTraceContext();
            tuxeContext.setTraceBeans(new ArrayList(1));
            context.setMqTraceContext(tuxeContext);
            tuxeContext.setTraceType(RMQTraceType.Pub);
            tuxeContext.setGroupName(NamespaceUtil.withoutNamespace(context.getProducerGroup(), context.getNamespace()));
            RMQTraceBean traceBean = new RMQTraceBean();
            traceBean.setTopic(NamespaceUtil.withoutNamespace(context.getMessage().getTopic(), context.getNamespace()));
            traceBean.setTags(context.getMessage().getTags());
            traceBean.setKeys(context.getMessage().getKeys());
            traceBean.setStoreHost(context.getBrokerAddr());
            traceBean.setBodyLength(context.getMessage().getBody().length);
            traceBean.setMsgType(context.getMsgType());
            tuxeContext.getTraceBeans().add(traceBean);
        }
    }

    @Override
    public void sendMessageAfter(SendMessageContext context) {
        if (context != null && !context.getMessage().getTopic().startsWith("RMQ_SYS_TRACE_TOPIC") && context.getMqTraceContext() != null && context.getSendResult() != null && context.getSendResult().getRegionId() != null && context.getSendResult().isTraceOn()) {
            RMQTraceContext tuxeContext = (RMQTraceContext) context.getMqTraceContext();
            RMQTraceBean traceBean = tuxeContext.getTraceBeans().get(0);
            int costTime = (int) ((System.currentTimeMillis() - tuxeContext.getTimeStamp()) / ((long) tuxeContext.getTraceBeans().size()));
            tuxeContext.setCostTime(costTime);
            if (context.getSendResult().getSendStatus().equals(SendStatus.SEND_OK)) {
                tuxeContext.setSuccess(true);
            } else {
                tuxeContext.setSuccess(false);
            }
            tuxeContext.setRegionId(context.getSendResult().getRegionId());
            traceBean.setMsgId(context.getSendResult().getMsgId());
            traceBean.setOffsetMsgId(context.getSendResult().getOffsetMsgId());
            traceBean.setStoreTime(tuxeContext.getTimeStamp() + ((long) (costTime / 2)));
            traceBean.setBornTime(System.currentTimeMillis());
            this.localDispatcher.append(tuxeContext);
        }
    }
}
