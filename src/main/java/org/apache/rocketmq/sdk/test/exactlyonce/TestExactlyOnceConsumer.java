package org.apache.rocketmq.sdk.test.exactlyonce;

import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.exactlyonce.ExactlyOnceConsumer;
import org.apache.rocketmq.sdk.api.exactlyonce.ExactlyOnceRMQFactory;
import java.util.Properties;

public class TestExactlyOnceConsumer {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.GROUP_ID, "GID_0601");
        properties.put("INSTANCE_ID", "MQ_INST_xxx");
        properties.put("AccessKey", "b4439888fd394c298292a2f7c31847a4");
        properties.put("SecretKey", "d7a79b7a8ca2407c8ea27dd9cf290aa4");
        properties.put(PropertyKeyConst.MsgTraceSwitch, true);
        properties.put(PropertyKeyConst.AuthenticationRequired, "true");
        properties.put(PropertyKeyConst.MaxReconsumeTimes, 3);
        properties.put("NAMESRV_ADDR", "xxx.xxx.xxx.xxx:9876");
        properties.setProperty(PropertyKeyConst.EXACTLYONCE_DELIVERY, "true");
        ExactlyOnceConsumer consumer = ExactlyOnceRMQFactory.createExactlyOnceConsumer(properties);
        consumer.subscribe("topic_0601", "*", new SimpleListener());
        consumer.start();
        System.out.println("Consumer start success.");
    }
}
