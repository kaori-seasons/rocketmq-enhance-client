package org.apache.rocketmq.sdk.api.exactlyonce;

import org.apache.rocketmq.sdk.api.RMQFactoryAPI;
import java.util.Properties;

public interface ExactlyOnceRMQFactoryAPI extends RMQFactoryAPI {
    ExactlyOnceConsumer createExactlyOnceConsumer(Properties properties);
}
