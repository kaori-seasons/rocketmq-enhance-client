package org.apache.rocketmq.sdk.api.impl.rocketmq;

import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.PropertyValueConst;
import org.apache.rocketmq.sdk.api.admin.Admin;
import org.apache.rocketmq.sdk.api.exception.RMQClientException;
import org.apache.rocketmq.sdk.api.impl.namespace.InstanceUtil;
import org.apache.rocketmq.sdk.api.impl.util.ClientLoggerUtil;
import org.apache.rocketmq.sdk.api.impl.util.NameAddrUtils;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.acl.SessionCredentials;
import org.apache.rocketmq.sdk.shade.common.namesrv.TopAddressing;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.trace.core.dispatch.AsyncDispatcher;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.backoff.ExponentialBackOff;

public abstract class RMQClientAbstract implements Admin {
    protected static final String WSADDR_INTERNAL = System.getProperty("com.#############", "http://###########");
    protected static final String WSADDR_INTERNET = System.getProperty("com.#############", "http://###########");
    protected static final long WSADDR_INTERNAL_TIMEOUTMILLS = Long.parseLong(System.getProperty("com.############", "3000"));
    protected static final long WSADDR_INTERNET_TIMEOUTMILLS = Long.parseLong(System.getProperty("com.##################", "5000"));
    private static final InternalLogger log = ClientLoggerUtil.getClientLogger();
    protected final Properties properties;
    protected String nameServerAddr;
    protected String proxyAddr;
    protected final String namespaceId;
    protected final SessionCredentials sessionCredentials = new SessionCredentials();
    protected AsyncDispatcher traceDispatcher = null;
    protected final AtomicBoolean started = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "TuxeClient-UpdateNameServerThread");
        }
    });

    protected abstract void updateNameServerAddr(String str);

    protected abstract void updateProxyAddr(String str);

    public RMQClientAbstract(Properties properties) {
        this.nameServerAddr = NameAddrUtils.getNameAdd();
        this.properties = properties;
        this.sessionCredentials.updateContent(properties);
        if (Boolean.parseBoolean(properties.getProperty(PropertyKeyConst.AuthenticationRequired))) {
            if (null == this.sessionCredentials.getAccessKey() || "".equals(this.sessionCredentials.getAccessKey())) {
                throw new RMQClientException("please set access key");
            } else if (null == this.sessionCredentials.getSecretKey() || "".equals(this.sessionCredentials.getSecretKey())) {
                throw new RMQClientException("please set secret key");
            }
        }
        this.namespaceId = parseNamespaceId();
        this.sessionCredentials.setNamespaceId(this.namespaceId);
        this.nameServerAddr = this.properties.getProperty("NAMESRV_ADDR");
        if (this.nameServerAddr == null) {
            this.proxyAddr = this.properties.getProperty("PROXY_ADDR");
            if (this.proxyAddr == null) {
                this.proxyAddr = fetchProxyAddr();
                if (null == this.proxyAddr) {
                    throw new RMQClientException(FAQ.errorMessage("Can not find proxy server, May be your network problem.", "http://#####"));
                }
                this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String psAddrs = RMQClientAbstract.this.fetchProxyAddr();
                            if (psAddrs != null && !RMQClientAbstract.this.proxyAddr.equals(psAddrs)) {
                                RMQClientAbstract.this.proxyAddr = psAddrs;
                                if (RMQClientAbstract.this.isStarted()) {
                                    RMQClientAbstract.this.updateProxyAddr(psAddrs);
                                }
                            }
                        } catch (Exception e) {
                            RMQClientAbstract.log.error("update name server periodically failed.", (Throwable) e);
                        }
                    }
                }, 10000, ExponentialBackOff.DEFAULT_MAX_INTERVAL, TimeUnit.MILLISECONDS);
            }
        }
    }

    public String fetchProxyAddr() {
        String tuxeAddr = this.properties.getProperty(PropertyKeyConst.RMQAddr);
        if (tuxeAddr != null) {
            String netType = this.properties.getProperty(PropertyKeyConst.NetType, PropertyValueConst.IPV4);
            if (netType.equals(PropertyValueConst.IPV4)) {
                tuxeAddr = tuxeAddr + "/ipv4";
            } else if (netType.equals(PropertyValueConst.IPV6)) {
                tuxeAddr = tuxeAddr + "/ipv6";
            }
            String psAddrs = new TopAddressing(tuxeAddr).fetchNSAddr();
            if (psAddrs != null) {
                log.info("connected to user-defined tuxedo addr server, {} success, {}", tuxeAddr, psAddrs);
                return psAddrs;
            }
            throw new RMQClientException(FAQ.errorMessage("Can not find proxy with tuxedoAddr " + tuxeAddr, "http://#####"));
        }
        String psAddrs2 = new TopAddressing(WSADDR_INTERNAL).fetchNSAddr(false, WSADDR_INTERNAL_TIMEOUTMILLS);
        if (psAddrs2 != null) {
            log.info("connected to internal server, {} success, {}", WSADDR_INTERNAL, psAddrs2);
            return psAddrs2;
        }
        String psAddrs3 = new TopAddressing(WSADDR_INTERNET).fetchNSAddr(false, WSADDR_INTERNET_TIMEOUTMILLS);
        if (psAddrs3 != null) {
            log.info("connected to internet server, {} success, {}", WSADDR_INTERNET, psAddrs3);
        }
        return psAddrs3;
    }

    public String getNameServerAddr() {
        return this.nameServerAddr;
    }

    public String getProxyAddr() {
        return this.proxyAddr;
    }

    protected String buildInstanceName() {
        return Integer.toString(UtilAll.getPid()) + "#" + (this.nameServerAddr == null ? this.proxyAddr.hashCode() : this.nameServerAddr.hashCode()) + "#" + (this.sessionCredentials.getAccessKey() != null ? Integer.valueOf(this.sessionCredentials.getAccessKey().hashCode()) : this.sessionCredentials.getAccessKey()) + "#" + System.nanoTime();
    }

    private String parseNamespaceId() {
        String namespaceId = null;
        String namespaceFromProperty = this.properties.getProperty("INSTANCE_ID", null);
        if (StringUtils.isNotEmpty(namespaceFromProperty)) {
            namespaceId = namespaceFromProperty;
            log.info("User specify namespaceId by property: {}.", namespaceId);
        }
        if (StringUtils.isBlank(namespaceId)) {
            return "";
        }
        return namespaceId;
    }

    protected String getNamespace() {
        return InstanceUtil.isIndependentNaming(this.namespaceId) ? this.namespaceId : "";
    }

    protected void checkONSProducerServiceState(DefaultMQProducerImpl producer) {
        switch (producer.getServiceState()) {
            case CREATE_JUST:
                throw new RMQClientException(FAQ.errorMessage(String.format("You do not have start the producer[" + UtilAll.getPid() + "], %s", producer.getServiceState()), "http://#####"));
            case SHUTDOWN_ALREADY:
                throw new RMQClientException(FAQ.errorMessage(String.format("Your producer has been shut down, %s", producer.getServiceState()), "http://#####"));
            case START_FAILED:
                throw new RMQClientException(FAQ.errorMessage(String.format("When you start your service throws an exception, %s", producer.getServiceState()), "http://#####"));
            case RUNNING:
            default:
        }
    }

    @Override
    public void start() {
        if (null != this.traceDispatcher) {
            try {
                this.traceDispatcher.start();
            } catch (MQClientException e) {
                log.warn("trace dispatcher start failed ", (Throwable) e);
            }
        }
    }

    @Override 
    public void updateCredential(Properties credentialProperties) {
        if (null == credentialProperties.getProperty("AccessKey") || "".equals(credentialProperties.getProperty("AccessKey"))) {
            throw new RMQClientException("update credential failed. please set access key.");
        } else if (null == credentialProperties.getProperty("SecretKey") || "".equals(credentialProperties.getProperty("SecretKey"))) {
            throw new RMQClientException("update credential failed. please set secret key");
        } else {
            this.sessionCredentials.updateContent(credentialProperties);
        }
    }

    @Override 
    public void shutdown() {
        if (null != this.traceDispatcher) {
            this.traceDispatcher.shutdown();
        }
        this.scheduledExecutorService.shutdown();
    }

    @Override 
    public boolean isStarted() {
        return this.started.get();
    }

    @Override 
    public boolean isClosed() {
        return !isStarted();
    }
}
