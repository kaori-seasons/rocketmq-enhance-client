package org.apache.rocketmq.sdk.trace.core.common;

import org.apache.rocketmq.sdk.shade.common.message.MessageClientIDSetter;
import java.util.List;

public class RMQTraceContext implements Comparable<RMQTraceContext> {
    private RMQTraceType traceType;
    private long timeStamp = System.currentTimeMillis();
    private String regionId = "";
    private String regionName = "";
    private String groupName = "";
    private int costTime = 0;
    private boolean isSuccess = true;
    private String requestId = MessageClientIDSetter.createUniqID();
    private int contextCode = 0;
    private List<RMQTraceBean> traceBeans;

    public int getContextCode() {
        return this.contextCode;
    }

    public void setContextCode(int contextCode) {
        this.contextCode = contextCode;
    }

    public List<RMQTraceBean> getTraceBeans() {
        return this.traceBeans;
    }

    public void setTraceBeans(List<RMQTraceBean> traceBeans) {
        this.traceBeans = traceBeans;
    }

    public String getRegionId() {
        return this.regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public RMQTraceType getTraceType() {
        return this.traceType;
    }

    public void setTraceType(RMQTraceType traceType) {
        this.traceType = traceType;
    }

    public long getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getGroupName() {
        return this.groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getCostTime() {
        return this.costTime;
    }

    public void setCostTime(int costTime) {
        this.costTime = costTime;
    }

    public boolean isSuccess() {
        return this.isSuccess;
    }

    public void setSuccess(boolean success) {
        this.isSuccess = success;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRegionName() {
        return this.regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public int compareTo(RMQTraceContext o) {
        return (int) (this.timeStamp - o.getTimeStamp());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(this.traceType).append("_").append(this.groupName).append("_").append(this.regionId).append("_").append(this.isSuccess).append("_");
        if (this.traceBeans != null && this.traceBeans.size() > 0) {
            for (RMQTraceBean bean : this.traceBeans) {
                sb.append(bean.getMsgId() + "_" + bean.getTopic() + "_");
            }
        }
        return "TuxeTraceContext{" + sb.toString() + '}';
    }
}
