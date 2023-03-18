package org.apache.rocketmq.sdk.shade.common.protocol;

import org.apache.rocketmq.sdk.shade.common.constant.LoggerName;
import org.apache.rocketmq.sdk.shade.common.protocol.header.namesrv.RegisterBrokerRequestHeader;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingHelper;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;

public class MQProtosHelper {
    private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.COMMON_LOGGER_NAME);

    public static boolean registerBrokerToNameServer(String nsaddr, String brokerAddr, long timeoutMillis) {
        RegisterBrokerRequestHeader requestHeader = new RegisterBrokerRequestHeader();
        requestHeader.setBrokerAddr(brokerAddr);
        try {
            RemotingCommand response = RemotingHelper.invokeSync(nsaddr, RemotingCommand.createRequestCommand(103, requestHeader), timeoutMillis);
            if (response != null) {
                return 0 == response.getCode();
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to register broker", (Throwable) e);
            return false;
        }
    }
}
