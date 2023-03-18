package org.apache.rocketmq.sdk.shade.client;

import com.alibaba.druid.wall.violation.ErrorCode;
import org.apache.rocketmq.sdk.shade.common.MixAll;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.protocol.NamespaceUtil;
import org.apache.rocketmq.sdk.shade.logging.inner.Level;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingUtil;
import org.apache.rocketmq.sdk.shade.remoting.netty.TlsSystemConfig;
import org.apache.rocketmq.sdk.shade.remoting.protocol.LanguageCode;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.PropertyAccessor;

public class ClientConfig {
    public static final String SEND_MESSAGE_WITH_VIP_CHANNEL_PROPERTY = "com.rocketmq.sendMessageWithVIPChannel";
    private String proxyAddr;
    protected String namespace;
    private String unitName;
    private String namesrvAddr = System.getProperty(MixAll.NAMESRV_ADDR_PROPERTY, System.getenv("NAMESRV_ADDR"));
    private String clientIP = RemotingUtil.getLocalAddress();
    private String instanceName = System.getProperty("rocketmq.client.name", "DEFAULT");
    private int clientCallbackExecutorThreads = Runtime.getRuntime().availableProcessors();
    private int pollNameServerInterval = Level.WARN_INT;
    private int heartbeatBrokerInterval = Level.WARN_INT;
    private int persistConsumerOffsetInterval = ErrorCode.UNION;
    private boolean unitMode = false;
    private boolean vipChannelEnabled = Boolean.parseBoolean(System.getProperty(SEND_MESSAGE_WITH_VIP_CHANNEL_PROPERTY, "true"));
    private boolean useTLS = TlsSystemConfig.tlsEnable;
    private LanguageCode language = LanguageCode.JAVA;

    public String buildMQClientId() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClientIP());
        sb.append("@");
        sb.append(getInstanceName());
        if (!UtilAll.isBlank(this.unitName)) {
            sb.append("@");
            sb.append(this.unitName);
        }
        return sb.toString();
    }

    public String getClientIP() {
        return this.clientIP;
    }

    public void setClientIP(String clientIP) {
        this.clientIP = clientIP;
    }

    public String getInstanceName() {
        return this.instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public void changeInstanceNameToPID() {
        if (this.instanceName.equals("DEFAULT")) {
            this.instanceName = String.valueOf(UtilAll.getPid());
        }
    }

    public String withNamespace(String resource) {
        return NamespaceUtil.wrapNamespace(getNamespace(), resource);
    }

    public Set<String> withNamespace(Set<String> resourceSet) {
        Set<String> resourceWithNamespace = new HashSet<>();
        for (String resource : resourceSet) {
            resourceWithNamespace.add(withNamespace(resource));
        }
        return resourceWithNamespace;
    }

    public String withoutNamespace(String resource) {
        return NamespaceUtil.withoutNamespace(resource, getNamespace());
    }

    public Set<String> withoutNamespace(Set<String> resourceSet) {
        Set<String> resourceWithoutNamespace = new HashSet<>();
        for (String resource : resourceSet) {
            resourceWithoutNamespace.add(withoutNamespace(resource));
        }
        return resourceWithoutNamespace;
    }

    public MessageQueue queueWithNamespace(MessageQueue queue) {
        if (StringUtils.isEmpty(getNamespace())) {
            return queue;
        }
        return new MessageQueue(withNamespace(queue.getTopic()), queue.getBrokerName(), queue.getQueueId());
    }

    public Collection<MessageQueue> queuesWithNamespace(Collection<MessageQueue> queues) {
        if (StringUtils.isEmpty(getNamespace())) {
            return queues;
        }
        for (MessageQueue queue : queues) {
            queue.setTopic(withNamespace(queue.getTopic()));
        }
        return queues;
    }

    public void resetClientConfig(ClientConfig cc) {
        this.namesrvAddr = cc.namesrvAddr;
        this.proxyAddr = cc.proxyAddr;
        this.clientIP = cc.clientIP;
        this.instanceName = cc.instanceName;
        this.clientCallbackExecutorThreads = cc.clientCallbackExecutorThreads;
        this.pollNameServerInterval = cc.pollNameServerInterval;
        this.heartbeatBrokerInterval = cc.heartbeatBrokerInterval;
        this.persistConsumerOffsetInterval = cc.persistConsumerOffsetInterval;
        this.unitMode = cc.unitMode;
        this.unitName = cc.unitName;
        this.vipChannelEnabled = cc.vipChannelEnabled;
        this.useTLS = cc.useTLS;
        this.namespace = cc.namespace;
        this.language = cc.language;
    }

    public ClientConfig cloneClientConfig() {
        ClientConfig cc = new ClientConfig();
        cc.namesrvAddr = this.namesrvAddr;
        cc.proxyAddr = this.proxyAddr;
        cc.clientIP = this.clientIP;
        cc.instanceName = this.instanceName;
        cc.clientCallbackExecutorThreads = this.clientCallbackExecutorThreads;
        cc.pollNameServerInterval = this.pollNameServerInterval;
        cc.heartbeatBrokerInterval = this.heartbeatBrokerInterval;
        cc.persistConsumerOffsetInterval = this.persistConsumerOffsetInterval;
        cc.unitMode = this.unitMode;
        cc.unitName = this.unitName;
        cc.vipChannelEnabled = this.vipChannelEnabled;
        cc.useTLS = this.useTLS;
        cc.namespace = this.namespace;
        cc.language = this.language;
        return cc;
    }

    public String getNamesrvAddr() {
        return this.namesrvAddr;
    }

    public void setNamesrvAddr(String namesrvAddr) {
        this.namesrvAddr = namesrvAddr;
    }

    public String getProxyAddr() {
        return this.proxyAddr;
    }

    public void setProxyAddr(String proxyAddr) {
        this.proxyAddr = proxyAddr;
    }

    public int getClientCallbackExecutorThreads() {
        return this.clientCallbackExecutorThreads;
    }

    public void setClientCallbackExecutorThreads(int clientCallbackExecutorThreads) {
        this.clientCallbackExecutorThreads = clientCallbackExecutorThreads;
    }

    public int getPollNameServerInterval() {
        return this.pollNameServerInterval;
    }

    public void setPollNameServerInterval(int pollNameServerInterval) {
        this.pollNameServerInterval = pollNameServerInterval;
    }

    public int getHeartbeatBrokerInterval() {
        return this.heartbeatBrokerInterval;
    }

    public void setHeartbeatBrokerInterval(int heartbeatBrokerInterval) {
        this.heartbeatBrokerInterval = heartbeatBrokerInterval;
    }

    public int getPersistConsumerOffsetInterval() {
        return this.persistConsumerOffsetInterval;
    }

    public void setPersistConsumerOffsetInterval(int persistConsumerOffsetInterval) {
        this.persistConsumerOffsetInterval = persistConsumerOffsetInterval;
    }

    public String getUnitName() {
        return this.unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public boolean isUnitMode() {
        return this.unitMode;
    }

    public void setUnitMode(boolean unitMode) {
        this.unitMode = unitMode;
    }

    public boolean isVipChannelEnabled() {
        return this.vipChannelEnabled;
    }

    public void setVipChannelEnabled(boolean vipChannelEnabled) {
        this.vipChannelEnabled = vipChannelEnabled;
    }

    public String getNamespace() {
        return this.namespace;
    }

    protected void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public boolean isUseTLS() {
        return this.useTLS;
    }

    public void setUseTLS(boolean useTLS) {
        this.useTLS = useTLS;
    }

    public LanguageCode getLanguage() {
        return this.language;
    }

    public void setLanguage(LanguageCode language) {
        this.language = language;
    }

    public String toString() {
        return "ClientConfig [namesrvAddr=" + this.namesrvAddr + ", clientIP=" + this.clientIP + ", instanceName=" + this.instanceName + ", namespace=" + this.namespace + ", clientCallbackExecutorThreads=" + this.clientCallbackExecutorThreads + ", pollNameServerInterval=" + this.pollNameServerInterval + ", heartbeatBrokerInterval=" + this.heartbeatBrokerInterval + ", persistConsumerOffsetInterval=" + this.persistConsumerOffsetInterval + ", unitMode=" + this.unitMode + ", unitName=" + this.unitName + ", vipChannelEnabled=" + this.vipChannelEnabled + ", useTLS=" + this.useTLS + ", language=" + this.language.name() + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
