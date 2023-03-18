package org.apache.rocketmq.sdk.trace.core.common;

import org.apache.rocketmq.sdk.shade.common.message.MessageType;
import java.util.ArrayList;
import java.util.List;

public class RMQTraceDataEncoder {
    public static List<RMQTraceContext> decoderFromTraceDataString(String traceData) {
        List<RMQTraceContext> resList = new ArrayList<>();
        if (traceData == null || traceData.length() <= 0) {
            return resList;
        }
        for (String context : traceData.split(String.valueOf((char) 2))) {
            String[] line = context.split(String.valueOf((char) 1));
            if (line[0].equals(RMQTraceType.Pub.name())) {
                RMQTraceContext pubContext = new RMQTraceContext();
                pubContext.setTraceType(RMQTraceType.Pub);
                pubContext.setTimeStamp(Long.parseLong(line[1]));
                pubContext.setRegionId(line[2]);
                pubContext.setGroupName(line[3]);
                RMQTraceBean bean = new RMQTraceBean();
                bean.setTopic(line[4]);
                bean.setMsgId(line[5]);
                bean.setTags(line[6]);
                bean.setKeys(line[7]);
                bean.setStoreHost(line[8]);
                bean.setBodyLength(Integer.parseInt(line[9]));
                pubContext.setCostTime(Integer.parseInt(line[10]));
                bean.setMsgType(MessageType.values()[Integer.parseInt(line[11])]);
                if (line.length == 13) {
                    pubContext.setSuccess(Boolean.parseBoolean(line[12]));
                } else if (line.length == 14) {
                    bean.setOffsetMsgId(line[12]);
                    pubContext.setSuccess(Boolean.parseBoolean(line[13]));
                }
                pubContext.setTraceBeans(new ArrayList(1));
                pubContext.getTraceBeans().add(bean);
                resList.add(pubContext);
            } else if (line[0].equals(RMQTraceType.SubBefore.name())) {
                RMQTraceContext subBeforeContext = new RMQTraceContext();
                subBeforeContext.setTraceType(RMQTraceType.SubBefore);
                subBeforeContext.setTimeStamp(Long.parseLong(line[1]));
                subBeforeContext.setRegionId(line[2]);
                subBeforeContext.setGroupName(line[3]);
                subBeforeContext.setRequestId(line[4]);
                RMQTraceBean bean2 = new RMQTraceBean();
                bean2.setMsgId(line[5]);
                bean2.setRetryTimes(Integer.parseInt(line[6]));
                bean2.setKeys(line[7]);
                subBeforeContext.setTraceBeans(new ArrayList(1));
                subBeforeContext.getTraceBeans().add(bean2);
                resList.add(subBeforeContext);
            } else if (line[0].equals(RMQTraceType.SubAfter.name())) {
                RMQTraceContext subAfterContext = new RMQTraceContext();
                subAfterContext.setTraceType(RMQTraceType.SubAfter);
                subAfterContext.setRequestId(line[1]);
                RMQTraceBean bean3 = new RMQTraceBean();
                bean3.setMsgId(line[2]);
                bean3.setKeys(line[5]);
                subAfterContext.setTraceBeans(new ArrayList(1));
                subAfterContext.getTraceBeans().add(bean3);
                subAfterContext.setCostTime(Integer.parseInt(line[3]));
                subAfterContext.setSuccess(Boolean.parseBoolean(line[4]));
                if (line.length >= 7) {
                    subAfterContext.setContextCode(Integer.parseInt(line[6]));
                }
                resList.add(subAfterContext);
            }
        }
        return resList;
    }

    public static RMQTraceTransferBean encoderFromContextBean(RMQTraceContext ctx) {
        if (ctx == null) {
            return null;
        }
        RMQTraceTransferBean transferBean = new RMQTraceTransferBean();
        StringBuilder sb = new StringBuilder(256);
        switch (ctx.getTraceType()) {
            case Pub:
                RMQTraceBean bean = ctx.getTraceBeans().get(0);
                sb.append(ctx.getTraceType()).append((char) 1).append(ctx.getTimeStamp()).append((char) 1).append(ctx.getRegionId()).append((char) 1).append(ctx.getGroupName()).append((char) 1).append(bean.getTopic()).append((char) 1).append(bean.getMsgId()).append((char) 1).append(bean.getTags()).append((char) 1).append(bean.getKeys()).append((char) 1).append(bean.getStoreHost()).append((char) 1).append(bean.getClientHost()).append((char) 1).append(bean.getBodyLength()).append((char) 1).append(ctx.getCostTime()).append((char) 1).append(bean.getMsgType().ordinal()).append((char) 1).append(bean.getOffsetMsgId()).append((char) 1).append(ctx.isSuccess()).append((char) 2);
                break;
            case SubBefore:
                for (RMQTraceBean bean2 : ctx.getTraceBeans()) {
                    sb.append(ctx.getTraceType()).append((char) 1).append(ctx.getTimeStamp()).append((char) 1).append(ctx.getRegionId()).append((char) 1).append(ctx.getGroupName()).append((char) 1).append(ctx.getRequestId()).append((char) 1).append(bean2.getMsgId()).append((char) 1).append(bean2.getRetryTimes()).append((char) 1).append(bean2.getKeys()).append((char) 2);
                }
                break;
            case SubAfter:
                for (RMQTraceBean bean3 : ctx.getTraceBeans()) {
                    sb.append(ctx.getTraceType()).append((char) 1).append(ctx.getTimeStamp()).append((char) 1).append(ctx.getRequestId()).append((char) 1).append(bean3.getMsgId()).append((char) 1).append(bean3.getClientHost()).append((char) 1).append(ctx.getGroupName()).append((char) 1).append(ctx.getCostTime()).append((char) 1).append(ctx.isSuccess()).append((char) 1).append(bean3.getKeys()).append((char) 1).append(ctx.getContextCode()).append((char) 2);
                }
                break;
        }
        transferBean.setTransData(sb.toString());
        for (RMQTraceBean bean4 : ctx.getTraceBeans()) {
            transferBean.getTransKey().add(bean4.getMsgId());
            if (bean4.getKeys() != null && bean4.getKeys().length() > 0) {
                transferBean.getTransKey().add(bean4.getKeys());
            }
        }
        return transferBean;
    }
}
