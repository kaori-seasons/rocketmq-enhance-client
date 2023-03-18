package org.apache.rocketmq.sdk.api.exactlyonce;

import org.apache.rocketmq.sdk.api.impl.RMQFactoryImpl;
import org.apache.rocketmq.sdk.api.impl.util.RMQUtil;
import java.util.Properties;

public class ExactlyOnceFactoryImpl extends RMQFactoryImpl implements ExactlyOnceRMQFactoryAPI {
    @Override
    public ExactlyOnceConsumer createExactlyOnceConsumer(Properties properties) {
        return new ExactlyOnceConsumerImpl(RMQUtil.extractProperties(properties));
    }
}
