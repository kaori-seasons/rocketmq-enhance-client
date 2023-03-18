package org.apache.rocketmq.sdk.shade.common;

import org.apache.rocketmq.sdk.shade.common.constant.PermName;
import org.springframework.beans.PropertyAccessor;

public class TopicConfig {
    private static final String SEPARATOR = " ";
    public static int defaultReadQueueNums = 16;
    public static int defaultWriteQueueNums = 16;
    private String topicName;
    private int readQueueNums;
    private int writeQueueNums;
    private int perm;
    private TopicFilterType topicFilterType;
    private int topicSysFlag;
    private boolean order;

    public TopicConfig() {
        this.readQueueNums = defaultReadQueueNums;
        this.writeQueueNums = defaultWriteQueueNums;
        this.perm = 6;
        this.topicFilterType = TopicFilterType.SINGLE_TAG;
        this.topicSysFlag = 0;
        this.order = false;
    }

    public TopicConfig(String topicName) {
        this.readQueueNums = defaultReadQueueNums;
        this.writeQueueNums = defaultWriteQueueNums;
        this.perm = 6;
        this.topicFilterType = TopicFilterType.SINGLE_TAG;
        this.topicSysFlag = 0;
        this.order = false;
        this.topicName = topicName;
    }

    public TopicConfig(String topicName, int readQueueNums, int writeQueueNums, int perm) {
        this.readQueueNums = defaultReadQueueNums;
        this.writeQueueNums = defaultWriteQueueNums;
        this.perm = 6;
        this.topicFilterType = TopicFilterType.SINGLE_TAG;
        this.topicSysFlag = 0;
        this.order = false;
        this.topicName = topicName;
        this.readQueueNums = readQueueNums;
        this.writeQueueNums = writeQueueNums;
        this.perm = perm;
    }

    public String encode() {
        return this.topicName + " " + this.readQueueNums + " " + this.writeQueueNums + " " + this.perm + " " + this.topicFilterType;
    }

    public boolean decode(String in) {
        String[] strs = in.split(" ");
        if (strs == null || strs.length != 5) {
            return false;
        }
        this.topicName = strs[0];
        this.readQueueNums = Integer.parseInt(strs[1]);
        this.writeQueueNums = Integer.parseInt(strs[2]);
        this.perm = Integer.parseInt(strs[3]);
        this.topicFilterType = TopicFilterType.valueOf(strs[4]);
        return true;
    }

    public String getTopicName() {
        return this.topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public int getReadQueueNums() {
        return this.readQueueNums;
    }

    public void setReadQueueNums(int readQueueNums) {
        this.readQueueNums = readQueueNums;
    }

    public int getWriteQueueNums() {
        return this.writeQueueNums;
    }

    public void setWriteQueueNums(int writeQueueNums) {
        this.writeQueueNums = writeQueueNums;
    }

    public int getPerm() {
        return this.perm;
    }

    public void setPerm(int perm) {
        this.perm = perm;
    }

    public TopicFilterType getTopicFilterType() {
        return this.topicFilterType;
    }

    public void setTopicFilterType(TopicFilterType topicFilterType) {
        this.topicFilterType = topicFilterType;
    }

    public int getTopicSysFlag() {
        return this.topicSysFlag;
    }

    public void setTopicSysFlag(int topicSysFlag) {
        this.topicSysFlag = topicSysFlag;
    }

    public boolean isOrder() {
        return this.order;
    }

    public void setOrder(boolean isOrder) {
        this.order = isOrder;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TopicConfig that = (TopicConfig) o;
        if (this.readQueueNums != that.readQueueNums || this.writeQueueNums != that.writeQueueNums || this.perm != that.perm || this.topicSysFlag != that.topicSysFlag || this.order != that.order) {
            return false;
        }
        if (this.topicName != null) {
            if (!this.topicName.equals(that.topicName)) {
                return false;
            }
        } else if (that.topicName != null) {
            return false;
        }
        return this.topicFilterType == that.topicFilterType;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * ((31 * ((31 * (this.topicName != null ? this.topicName.hashCode() : 0)) + this.readQueueNums)) + this.writeQueueNums)) + this.perm)) + (this.topicFilterType != null ? this.topicFilterType.hashCode() : 0))) + this.topicSysFlag)) + (this.order ? 1 : 0);
    }

    public String toString() {
        return "TopicConfig [topicName=" + this.topicName + ", readQueueNums=" + this.readQueueNums + ", writeQueueNums=" + this.writeQueueNums + ", perm=" + PermName.perm2String(this.perm) + ", topicFilterType=" + this.topicFilterType + ", topicSysFlag=" + this.topicSysFlag + ", order=" + this.order + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
