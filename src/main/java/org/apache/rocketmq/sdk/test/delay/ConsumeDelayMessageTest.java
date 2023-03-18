package org.apache.rocketmq.sdk.test.delay;

import org.apache.rocketmq.sdk.api.Action;
import org.apache.rocketmq.sdk.api.ConsumeContext;
import org.apache.rocketmq.sdk.api.Consumer;
import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.MessageListener;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.PropertyValueConst;
import org.apache.rocketmq.sdk.api.RMQFactory;
import java.util.Date;
import java.util.Properties;
import org.apache.commons.lang3.time.DateFormatUtils;

public class ConsumeDelayMessageTest {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.GROUP_ID, "GID_delay");
        properties.put("INSTANCE_ID", "MQ_INST_xxx");
        properties.put("AccessKey", "b4439888fd394c298292a2f7c318470d");
        properties.put("SecretKey", "d7a79b7a8ca2407c8ea27dd9cf290ac6");
        properties.put(PropertyKeyConst.MsgTraceSwitch, true);
        properties.put(PropertyKeyConst.AuthenticationRequired, "true");
        properties.put("PROXY_ADDR", "xxx.xxx.xxx.xxx:8081");
        properties.put(PropertyKeyConst.MaxReconsumeTimes, 3);
        properties.put(PropertyKeyConst.MessageModel, PropertyValueConst.CLUSTERING);
        Consumer traceConsumer = RMQFactory.createConsumer(properties);
        traceConsumer.subscribe("topic_delay", "*", new MessageListener() {
            @Override
            public Action consume(Message message, ConsumeContext context) {
                System.out.println();
                System.out.println("Receive topic_delay message " + message.getMsgID() + ", consumeMessage time:" + DateFormatUtils.format(new Date(), "yyyy-MM-ss HH:mm:ss"));
                return Action.CommitMessage;
            }
        });
        traceConsumer.start();
    }
}
