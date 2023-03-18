package org.apache.rocketmq.sdk.shade.common.protocol.heartbeat;

import com.alibaba.fastjson.annotation.JSONField;
import org.apache.rocketmq.sdk.shade.common.filter.ExpressionType;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.PropertyAccessor;

public class SubscriptionData implements Comparable<SubscriptionData> {
    public static final String SUB_ALL = "*";
    private String topic;
    private String subString;
    @JSONField(serialize = false)
    private String filterClassSource;
    private boolean classFilterMode = false;
    private Set<String> tagsSet = new HashSet();
    private Set<Integer> codeSet = new HashSet();
    private long subVersion = System.currentTimeMillis();
    private String expressionType = ExpressionType.TAG;

    public SubscriptionData() {
    }

    public SubscriptionData(String topic, String subString) {
        this.topic = topic;
        this.subString = subString;
    }

    public String getFilterClassSource() {
        return this.filterClassSource;
    }

    public void setFilterClassSource(String filterClassSource) {
        this.filterClassSource = filterClassSource;
    }

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getSubString() {
        return this.subString;
    }

    public void setSubString(String subString) {
        this.subString = subString;
    }

    public Set<String> getTagsSet() {
        return this.tagsSet;
    }

    public void setTagsSet(Set<String> tagsSet) {
        this.tagsSet = tagsSet;
    }

    public long getSubVersion() {
        return this.subVersion;
    }

    public void setSubVersion(long subVersion) {
        this.subVersion = subVersion;
    }

    public Set<Integer> getCodeSet() {
        return this.codeSet;
    }

    public void setCodeSet(Set<Integer> codeSet) {
        this.codeSet = codeSet;
    }

    public boolean isClassFilterMode() {
        return this.classFilterMode;
    }

    public void setClassFilterMode(boolean classFilterMode) {
        this.classFilterMode = classFilterMode;
    }

    public String getExpressionType() {
        return this.expressionType;
    }

    public void setExpressionType(String expressionType) {
        this.expressionType = expressionType;
    }

    @Override
    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * ((31 * ((31 * 1) + (this.classFilterMode ? 1231 : 1237))) + (this.codeSet == null ? 0 : this.codeSet.hashCode()))) + (this.subString == null ? 0 : this.subString.hashCode()))) + (this.tagsSet == null ? 0 : this.tagsSet.hashCode()))) + (this.topic == null ? 0 : this.topic.hashCode()))) + (this.expressionType == null ? 0 : this.expressionType.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SubscriptionData other = (SubscriptionData) obj;
        if (this.classFilterMode != other.classFilterMode) {
            return false;
        }
        if (this.codeSet == null) {
            if (other.codeSet != null) {
                return false;
            }
        } else if (!this.codeSet.equals(other.codeSet)) {
            return false;
        }
        if (this.subString == null) {
            if (other.subString != null) {
                return false;
            }
        } else if (!this.subString.equals(other.subString)) {
            return false;
        }
        if (this.subVersion != other.subVersion) {
            return false;
        }
        if (this.tagsSet == null) {
            if (other.tagsSet != null) {
                return false;
            }
        } else if (!this.tagsSet.equals(other.tagsSet)) {
            return false;
        }
        if (this.topic == null) {
            if (other.topic != null) {
                return false;
            }
        } else if (!this.topic.equals(other.topic)) {
            return false;
        }
        if (this.expressionType == null) {
            if (other.expressionType != null) {
                return false;
            }
            return true;
        } else if (!this.expressionType.equals(other.expressionType)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public String toString() {
        return "SubscriptionData [classFilterMode=" + this.classFilterMode + ", topic=" + this.topic + ", subString=" + this.subString + ", tagsSet=" + this.tagsSet + ", codeSet=" + this.codeSet + ", subVersion=" + this.subVersion + ", expressionType=" + this.expressionType + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }

    public int compareTo(SubscriptionData other) {
        return (this.topic + "@" + this.subString).compareTo(other.topic + "@" + other.subString);
    }
}
