package org.apache.rocketmq.sdk.shade.common.message;

public class MessageClientExt extends MessageExt {
    public String getOffsetMsgId() {
        return super.getMsgId();
    }

    public void setOffsetMsgId(String offsetMsgId) {
        super.setMsgId(offsetMsgId);
    }

    @Override
    public String getMsgId() {
        String uniqID = MessageClientIDSetter.getUniqID(this);
        if (uniqID == null) {
            return getOffsetMsgId();
        }
        return uniqID;
    }

    @Override
    public void setMsgId(String msgId) {
    }
}
