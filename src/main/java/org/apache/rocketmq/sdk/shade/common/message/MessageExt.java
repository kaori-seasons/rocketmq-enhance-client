package org.apache.rocketmq.sdk.shade.common.message;

import org.apache.rocketmq.sdk.shade.common.TopicFilterType;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import org.springframework.beans.PropertyAccessor;

public class MessageExt extends Message {
    private static final long serialVersionUID = 5720810158625748049L;
    private String brokerName;
    private int queueId;
    private int storeSize;
    private long queueOffset;
    private int sysFlag;
    private long bornTimestamp;
    private SocketAddress bornHost;
    private long storeTimestamp;
    private SocketAddress storeHost;
    private String msgId;
    private long commitLogOffset;
    private int bodyCRC;
    private int reconsumeTimes;
    private long preparedTransactionOffset;

    public MessageExt() {
    }

    public MessageExt(int queueId, long bornTimestamp, SocketAddress bornHost, long storeTimestamp, SocketAddress storeHost, String msgId) {
        this.queueId = queueId;
        this.bornTimestamp = bornTimestamp;
        this.bornHost = bornHost;
        this.storeTimestamp = storeTimestamp;
        this.storeHost = storeHost;
        this.msgId = msgId;
    }

    public static TopicFilterType parseTopicFilterType(int sysFlag) {
        if ((sysFlag & 2) == 2) {
            return TopicFilterType.MULTI_TAG;
        }
        return TopicFilterType.SINGLE_TAG;
    }

    public static ByteBuffer socketAddress2ByteBuffer(SocketAddress socketAddress, ByteBuffer byteBuffer) {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        byteBuffer.put(inetSocketAddress.getAddress().getAddress(), 0, 4);
        byteBuffer.putInt(inetSocketAddress.getPort());
        byteBuffer.flip();
        return byteBuffer;
    }

    public static ByteBuffer socketAddress2ByteBuffer(SocketAddress socketAddress) {
        return socketAddress2ByteBuffer(socketAddress, ByteBuffer.allocate(8));
    }

    public ByteBuffer getBornHostBytes() {
        return socketAddress2ByteBuffer(this.bornHost);
    }

    public ByteBuffer getBornHostBytes(ByteBuffer byteBuffer) {
        return socketAddress2ByteBuffer(this.bornHost, byteBuffer);
    }

    public ByteBuffer getStoreHostBytes() {
        return socketAddress2ByteBuffer(this.storeHost);
    }

    public ByteBuffer getStoreHostBytes(ByteBuffer byteBuffer) {
        return socketAddress2ByteBuffer(this.storeHost, byteBuffer);
    }

    public int getQueueId() {
        return this.queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public long getBornTimestamp() {
        return this.bornTimestamp;
    }

    public void setBornTimestamp(long bornTimestamp) {
        this.bornTimestamp = bornTimestamp;
    }

    public SocketAddress getBornHost() {
        return this.bornHost;
    }

    public void setBornHost(SocketAddress bornHost) {
        this.bornHost = bornHost;
    }

    public String getBornHostString() {
        if (this.bornHost != null) {
            return ((InetSocketAddress) this.bornHost).getAddress().getHostAddress();
        }
        return null;
    }

    public String getBornHostNameString() {
        if (this.bornHost != null) {
            return ((InetSocketAddress) this.bornHost).getAddress().getHostName();
        }
        return null;
    }

    public long getStoreTimestamp() {
        return this.storeTimestamp;
    }

    public void setStoreTimestamp(long storeTimestamp) {
        this.storeTimestamp = storeTimestamp;
    }

    public SocketAddress getStoreHost() {
        return this.storeHost;
    }

    public void setStoreHost(SocketAddress storeHost) {
        this.storeHost = storeHost;
    }

    public String getMsgId() {
        return this.msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public int getSysFlag() {
        return this.sysFlag;
    }

    public void setSysFlag(int sysFlag) {
        this.sysFlag = sysFlag;
    }

    public int getBodyCRC() {
        return this.bodyCRC;
    }

    public void setBodyCRC(int bodyCRC) {
        this.bodyCRC = bodyCRC;
    }

    public long getQueueOffset() {
        return this.queueOffset;
    }

    public void setQueueOffset(long queueOffset) {
        this.queueOffset = queueOffset;
    }

    public long getCommitLogOffset() {
        return this.commitLogOffset;
    }

    public void setCommitLogOffset(long physicOffset) {
        this.commitLogOffset = physicOffset;
    }

    public int getStoreSize() {
        return this.storeSize;
    }

    public void setStoreSize(int storeSize) {
        this.storeSize = storeSize;
    }

    public int getReconsumeTimes() {
        return this.reconsumeTimes;
    }

    public void setReconsumeTimes(int reconsumeTimes) {
        this.reconsumeTimes = reconsumeTimes;
    }

    public long getPreparedTransactionOffset() {
        return this.preparedTransactionOffset;
    }

    public void setPreparedTransactionOffset(long preparedTransactionOffset) {
        this.preparedTransactionOffset = preparedTransactionOffset;
    }

    public String getBrokerName() {
        return this.brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public long getDecodedTime() {
        String decodedTimeString = getProperty(MessageConst.PROPERTY_DECODED_TIMESTAMP);
        if (null == decodedTimeString) {
            return 0;
        }
        return Long.parseLong(decodedTimeString);
    }

    @Override
    public String toString() {
        return "MessageExt [brokerName=" + this.brokerName + ", queueId=" + this.queueId + ", storeSize=" + this.storeSize + ", queueOffset=" + this.queueOffset + ", sysFlag=" + this.sysFlag + ", bornTimestamp=" + this.bornTimestamp + ", bornHost=" + this.bornHost + ", storeTimestamp=" + this.storeTimestamp + ", storeHost=" + this.storeHost + ", msgId=" + this.msgId + ", commitLogOffset=" + this.commitLogOffset + ", bodyCRC=" + this.bodyCRC + ", reconsumeTimes=" + this.reconsumeTimes + ", preparedTransactionOffset=" + this.preparedTransactionOffset + ", toString()=" + super.toString() + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
