package org.apache.rocketmq.sdk.shade.common.protocol.body;

public class ConsumeQueueData {
    private long physicOffset;
    private int physicSize;
    private long tagsCode;
    private String extendDataJson;
    private String bitMap;
    private boolean eval;
    private String msg;

    public long getPhysicOffset() {
        return this.physicOffset;
    }

    public void setPhysicOffset(long physicOffset) {
        this.physicOffset = physicOffset;
    }

    public int getPhysicSize() {
        return this.physicSize;
    }

    public void setPhysicSize(int physicSize) {
        this.physicSize = physicSize;
    }

    public long getTagsCode() {
        return this.tagsCode;
    }

    public void setTagsCode(long tagsCode) {
        this.tagsCode = tagsCode;
    }

    public String getExtendDataJson() {
        return this.extendDataJson;
    }

    public void setExtendDataJson(String extendDataJson) {
        this.extendDataJson = extendDataJson;
    }

    public String getBitMap() {
        return this.bitMap;
    }

    public void setBitMap(String bitMap) {
        this.bitMap = bitMap;
    }

    public boolean isEval() {
        return this.eval;
    }

    public void setEval(boolean eval) {
        this.eval = eval;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String toString() {
        return "ConsumeQueueData{physicOffset=" + this.physicOffset + ", physicSize=" + this.physicSize + ", tagsCode=" + this.tagsCode + ", extendDataJson='" + this.extendDataJson + "', bitMap='" + this.bitMap + "', eval=" + this.eval + ", msg='" + this.msg + "'}";
    }
}
