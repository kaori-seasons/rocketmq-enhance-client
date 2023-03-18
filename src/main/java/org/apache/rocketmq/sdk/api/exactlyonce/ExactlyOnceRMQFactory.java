package org.apache.rocketmq.sdk.api.exactlyonce;

import org.apache.rocketmq.sdk.api.RMQFactory;
import java.util.Properties;

public class ExactlyOnceRMQFactory extends RMQFactory {
    private static ExactlyOnceRMQFactoryAPI exactlyOnceRMQFactoryAPI = new ExactlyOnceFactoryImpl();

    public static ExactlyOnceConsumer createExactlyOnceConsumer(Properties properties) {
        return exactlyOnceRMQFactoryAPI.createExactlyOnceConsumer(properties);
    }
}
