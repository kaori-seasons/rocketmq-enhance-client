package org.apache.rocketmq.sdk.api;

public class PropertyKeyConst {
    public static final String MessageModel = "MessageModel";
    @Deprecated
    public static final String ProducerId = "ProducerId";
    @Deprecated
    public static final String ConsumerId = "ConsumerId";
    public static final String INSTANCE_ID = "INSTANCE_ID";
    public static final String GROUP_ID = "GROUP_ID";
    public static final String AccessKey = "AccessKey";
    public static final String SecretKey = "SecretKey";
    public static final String AuthenticationRequired = "AuthenticationRequired";
    public static final String SecurityToken = "SecurityToken";
    public static final String SendMsgTimeoutMillis = "SendMsgTimeoutMillis";
    public static final String isTlsEnable = "isTlsEnable";
    public static final String RMQAddr = "RMQAddr";
    public static final String NAMESRV_ADDR = "NAMESRV_ADDR";
    public static final String PROXY_ADDR = "PROXY_ADDR";
    public static final String ConsumeThreadNums = "ConsumeThreadNums";
    public static final String OnsChannel = "OnsChannel";
    public static final String MQType = "MQType";
    public static final String isVipChannelEnabled = "isVipChannelEnabled";
    public static final String SuspendTimeMillis = "suspendTimeMillis";
    public static final String MaxReconsumeTimes = "maxReconsumeTimes";
    public static final String ConsumeTimeout = "consumeTimeout";
    public static final String BatchConsumeMaxAwaitDurationInSeconds = "batchConsumeMaxAwaitDurationInSeconds";
    public static final String CheckImmunityTimeInSeconds = "CheckImmunityTimeInSeconds";
    public static final String PostSubscriptionWhenPull = "PostSubscriptionWhenPull";
    public static final String ConsumeMessageBatchMaxSize = "ConsumeMessageBatchMaxSize";
    public static final String MaxCachedMessageAmount = "maxCachedMessageAmount";
    public static final String MaxCachedMessageSizeInMiB = "maxCachedMessageSizeInMiB";
    public static final String InstanceName = "InstanceName";
    public static final String MsgTraceSwitch = "MsgTraceSwitch";
    public static final String MqttMessageId = "mqttMessageId";
    public static final String MqttMessage = "mqttMessage";
    public static final String MqttPublishRetain = "mqttRetain";
    public static final String MqttPublishDubFlag = "mqttPublishDubFlag";
    public static final String MqttSecondTopic = "mqttSecondTopic";
    public static final String MqttClientId = "clientId";
    public static final String MqttQOS = "qoslevel";
    public static final String NetType = "netType";
    public static final String EXACTLYONCE_DELIVERY = "exactlyOnceDelivery";
    public static final String EXACTLYONCE_RM_REFRESHINTERVAL = "exactlyOnceRmRefreshInterval";
}
